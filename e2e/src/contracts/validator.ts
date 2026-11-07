import { ProblemDetail } from './types.js';

export class ContractValidator {
  /**
   * Asserts that an error response matches RFC 7807 Problem Details
   */
  static assertProblemDetail(data: any, expectedStatus?: number): asserts data is ProblemDetail {
    if (!data || typeof data !== 'object') {
      throw new Error(`Expected ProblemDetail object, got: ${typeof data}`);
    }
    if (typeof data.title !== 'string' || data.title.trim().length === 0) {
      throw new Error(`ProblemDetail missing or empty title: ${JSON.stringify(data)}`);
    }
    if (typeof data.status !== 'number') {
      throw new Error(`ProblemDetail missing status code: ${JSON.stringify(data)}`);
    }
    if (expectedStatus && data.status !== expectedStatus) {
      throw new Error(`Expected status ${expectedStatus}, got ${data.status} in ProblemDetail: ${JSON.stringify(data)}`);
    }
    if (typeof data.detail !== 'string') {
      throw new Error(`ProblemDetail missing detail string: ${JSON.stringify(data)}`);
    }
  }

  /**
   * Validates UUID format v4
   */
  static isUUID(str: string): boolean {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    return uuidRegex.test(str);
  }

  /**
   * Validates ISO 8601 Timestamp
   */
  static isISODate(str: string): boolean {
    const date = new Date(str);
    return !isNaN(date.getTime()) && str.includes('T');
  }
}
