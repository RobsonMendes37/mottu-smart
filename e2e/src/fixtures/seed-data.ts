import { UserRole, MotorcycleModel, MotorcycleStatus } from '../contracts/enums.js';

export const SEED_USERS = {
  MANAGER: {
    email: 'gestor@mottu-ops.local',
    password: 'Mottu@2026!',
    role: UserRole.MANAGER,
    fullName: 'Gestor Operações',
  },
  MECHANIC: {
    email: 'mecanico@mottu-ops.local',
    password: 'Mottu@2026!',
    role: UserRole.MECHANIC,
    fullName: 'Mecânico Chefe',
  },
  RIDER: {
    email: 'entregador@mottu-ops.local',
    password: 'Mottu@2026!',
    role: UserRole.RIDER,
    fullName: 'Carlos Entregador',
    assignedPlate: 'MOT1A23',
    assignedModel: MotorcycleModel.MOTTU_SPORT_110I,
    initialOdometerKm: 1000,
  },
};

export const SEED_YARDS = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    name: 'Galpão Mottu Butantã',
    city: 'São Paulo',
    address: 'Av. Corifeu de Azevedo Marques, 1000',
    active: true,
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    name: 'Galpão Mottu Guarulhos',
    city: 'Guarulhos',
    address: 'Rodovia Presidente Dutra, km 220',
    active: true,
  },
];

export const SEED_MOTORCYCLES = [
  {
    id: '33333333-3333-3333-3333-333333333333',
    plate: 'MOT1A23',
    model: MotorcycleModel.MOTTU_SPORT_110I,
    yardId: SEED_YARDS[0].id,
    yardName: SEED_YARDS[0].name,
    status: MotorcycleStatus.RENTED,
    currentOdometerKm: 1035,
    activeRiderName: 'Carlos Entregador',
  },
  {
    id: '44444444-4444-4444-4444-444444444444',
    plate: 'MOT2B34',
    model: MotorcycleModel.MOTTU_SPORT_110I,
    yardId: SEED_YARDS[0].id,
    yardName: SEED_YARDS[0].name,
    status: MotorcycleStatus.AVAILABLE,
    currentOdometerKm: 250,
    activeRiderName: null,
  },
  {
    id: '55555555-5555-5555-5555-555555555555',
    plate: 'POP3C45',
    model: MotorcycleModel.HONDA_POP_110I,
    yardId: SEED_YARDS[1].id,
    yardName: SEED_YARDS[1].name,
    status: MotorcycleStatus.AVAILABLE,
    currentOdometerKm: 1200,
    activeRiderName: null,
  },
  {
    id: '66666666-6666-6666-6666-666666666666',
    plate: 'TVS4D56',
    model: MotorcycleModel.TVS_APACHE_160,
    yardId: SEED_YARDS[0].id,
    yardName: SEED_YARDS[0].name,
    status: MotorcycleStatus.AVAILABLE,
    currentOdometerKm: 50,
    activeRiderName: null,
  },
];

export const SEED_PARTS = [
  {
    id: '77777777-7777-7777-7777-777777777771',
    vtexSku: 'SKU-MANETE-DIR-SPORT',
    motorcycleModel: MotorcycleModel.MOTTU_SPORT_110I,
    partName: 'Manete de Freio Direito',
    category: 'Comandos & Freios',
    unitPriceBrl: 25.0,
    isNaturalWear: false,
    priceOverridden: false,
  },
  {
    id: '77777777-7777-7777-7777-777777777772',
    vtexSku: 'SKU-LONA-FREIO-TRAS-SPORT',
    motorcycleModel: MotorcycleModel.MOTTU_SPORT_110I,
    partName: 'Lona de Freio Traseira',
    category: 'Freios',
    unitPriceBrl: 33.0,
    isNaturalWear: true,
    priceOverridden: false,
  },
  {
    id: '77777777-7777-7777-7777-777777777773',
    vtexSku: 'SKU-RETROVISOR-DIR-SPORT',
    motorcycleModel: MotorcycleModel.MOTTU_SPORT_110I,
    partName: 'Retrovisor Direito',
    category: 'Carenagem & Espelhos',
    unitPriceBrl: 42.5,
    isNaturalWear: false,
    priceOverridden: false,
  },
  {
    id: '77777777-7777-7777-7777-777777777774',
    vtexSku: 'SKU-PNEU-TRAS-SPORT',
    motorcycleModel: MotorcycleModel.MOTTU_SPORT_110I,
    partName: 'Pneu Traseiro 80/100-14',
    category: 'Rodas & Pneus',
    unitPriceBrl: 180.0,
    isNaturalWear: true,
    priceOverridden: false,
  },
];

export const STANDARD_SETTINGS = {
  standardLaborFeeBrl: 35.0,
  aiConfidenceThreshold: 0.8,
  preExistingMaxHours: 48,
  preExistingMaxKm: 100,
};
