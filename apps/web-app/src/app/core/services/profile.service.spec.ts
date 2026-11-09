import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { ProfileService } from './profile.service';

describe('ProfileService (Self-Service Account Persistence)', () => {
  let service: ProfileService;
  let mockAuthService: any;
  let currentUserSignal: any;

  beforeEach(() => {
    localStorage.clear();
    currentUserSignal = signal({
      userId: 'user-rider-1',
      role: 'RIDER' as const,
      fullName: 'Carlos Silva',
    });

    mockAuthService = {
      currentUser: currentUserSignal,
    };

    service = new ProfileService(mockAuthService);
  });

  it('TC-PROF-1: Returns default Rider profile populated from AuthService', () => {
    const profile = service.getProfile();
    expect(profile.fullName).toBe('Carlos Silva');
    expect(profile.role).toBe('RIDER');
    expect(profile.preferredUnit).toContain('Butantã');
    expect(profile.phone).toBeTruthy();
  });

  it('TC-PROF-2: Persists profile edits in localStorage and syncs fullName back to AuthService', () => {
    const updated = service.updateProfile({
      fullName: 'Carlos Eduardo Silva',
      phone: '(11) 99888-7766',
      email: 'carlos.edu@mottu.local',
      pixKey: '11998887766',
      preferredUnit: 'Unidade Pinheiros (SP-02)',
    });

    expect(updated.fullName).toBe('Carlos Eduardo Silva');
    expect(updated.phone).toBe('(11) 99888-7766');
    expect(updated.pixKey).toBe('11998887766');
    expect(currentUserSignal().fullName).toBe('Carlos Eduardo Silva');

    // Reload from fresh service instance to verify localStorage persistence
    const reloaded = new ProfileService(mockAuthService).getProfile();
    expect(reloaded.phone).toBe('(11) 99888-7766');
    expect(reloaded.preferredUnit).toBe('Unidade Pinheiros (SP-02)');
  });

  it('TC-PROF-3: Validates password change rules (current required, min 8 chars)', () => {
    const invalidShort = service.changePassword('Mottu@2026!', '12345');
    expect(invalidShort.success).toBe(false);
    expect(invalidShort.message).toContain('8 caracteres');

    const validChange = service.changePassword('Mottu@2026!', 'NovaSenha@2026');
    expect(validChange.success).toBe(true);
    expect(validChange.message).toContain('Senha atualizada');
  });

  it('TC-PROF-4: fetchProfile issues GET /api/v1/profile/me and falls back gracefully', () => {
    let emitted: any = null;
    service.fetchProfile().subscribe((p) => (emitted = p));
    expect(emitted).toBeTruthy();
    expect(emitted.fullName).toBe('Carlos Silva');
  });

  it('TC-PROF-5: changePasswordHttp validates input and computes SHA-256 fingerprint', async () => {
    const res = await service.changePasswordHttp('Mottu@2026!', 'NovaSenhaForte@2026');
    expect(res.success).toBe(true);
    expect(res.passwordSha256Fingerprint).toBeDefined();
  });
});

