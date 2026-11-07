import { describe, it, expect, vi, beforeEach } from 'vitest';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { AccidentMechanicService } from './accident-mechanic.service';
import { ConfirmAccidentReportCommand } from '../models/accident-dossier.models';

describe('AccidentMechanicService', () => {
  let service: AccidentMechanicService;
  let httpMock: any;

  beforeEach(() => {
    httpMock = {
      get: vi.fn(),
      post: vi.fn(),
    };
    service = new AccidentMechanicService(httpMock as HttpClient);
  });

  it('TC-MECH-ACC-1: Fetches accidents queue with pagination', () => {
    const mockPage = { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
    httpMock.get.mockReturnValue(of(mockPage));

    service.getAccidentsQueue(0, 20).subscribe((res) => {
      expect(res).toEqual(mockPage);
      expect(httpMock.get).toHaveBeenCalledWith('/api/v1/mechanic/accidents', expect.any(Object));
    });
  });

  it('TC-MECH-ACC-2: Fetches accident dossier details', () => {
    const mockDetails = { id: 'rep-1', plate: 'MOT1A23', status: 'ESTIMATED_PENDING_WORKSHOP' };
    httpMock.get.mockReturnValue(of(mockDetails));

    service.getAccidentDetails('rep-1').subscribe((res) => {
      expect(res.id).toBe('rep-1');
      expect(httpMock.get).toHaveBeenCalledWith('/api/v1/mechanic/accidents/rep-1');
    });
  });

  it('TC-MECH-ACC-3: Confirms accident maintenance and sends additional parts', () => {
    const mockResponse = { accidentReportId: 'rep-1', status: 'WORKSHOP_CONFIRMED', totalEstimatedBrl: 350.0 };
    httpMock.post.mockReturnValue(of(mockResponse));

    const cmd: ConfirmAccidentReportCommand = {
      confirmedPartIds: ['p-1'],
      additionalCatalogPartIds: ['p-2'],
      mechanicNotes: 'Aprovado',
    };

    service.confirmAccident('rep-1', cmd).subscribe((res) => {
      expect(res.status).toBe('WORKSHOP_CONFIRMED');
      expect(httpMock.post).toHaveBeenCalledWith('/api/v1/mechanic/accidents/rep-1/confirm', cmd);
    });
  });
});

