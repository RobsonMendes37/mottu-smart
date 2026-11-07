import { test as baseTest, expect } from '@playwright/test';
import { MockContractServer } from '../mock-server/contract-server.js';
import { MottuApiClient } from '../client/api-client.js';
import { SEED_USERS } from './seed-data.js';

let sharedMockServer: MockContractServer | null = null;
let sharedServerUrl: string | null = null;

export interface TestFixtures {
  serverUrl: string;
  mockServer: MockContractServer | null;
  riderClient: MottuApiClient;
  mechanicClient: MottuApiClient;
  managerClient: MottuApiClient;
  unauthClient: MottuApiClient;
}

export const test = baseTest.extend<TestFixtures>({
  serverUrl: async ({}, use) => {
    const mode = process.env.E2E_CONTRACT_MODE || 'mock';
    if (mode === 'live') {
      const liveUrl = process.env.E2E_BASE_URL || 'http://localhost:18088';
      await use(liveUrl);
    } else {
      if (!sharedMockServer) {
        sharedMockServer = new MockContractServer(18099);
        sharedServerUrl = await sharedMockServer.start();
      }
      await use(sharedServerUrl!);
    }
  },

  mockServer: async ({}, use) => {
    use(sharedMockServer);
  },

  riderClient: async ({ serverUrl }, use) => {
    const client = new MottuApiClient(serverUrl);
    await client.login({
      email: SEED_USERS.RIDER.email,
      password: SEED_USERS.RIDER.password,
    });
    await use(client);
  },

  mechanicClient: async ({ serverUrl }, use) => {
    const client = new MottuApiClient(serverUrl);
    await client.login({
      email: SEED_USERS.MECHANIC.email,
      password: SEED_USERS.MECHANIC.password,
    });
    await use(client);
  },

  managerClient: async ({ serverUrl }, use) => {
    const client = new MottuApiClient(serverUrl);
    await client.login({
      email: SEED_USERS.MANAGER.email,
      password: SEED_USERS.MANAGER.password,
    });
    await use(client);
  },

  unauthClient: async ({ serverUrl }, use) => {
    const client = new MottuApiClient(serverUrl);
    await use(client);
  },
});

export { expect };
