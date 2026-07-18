export enum AccountType {
  SAVINGS = 'SAVINGS',
  CHECKING = 'CHECKING',
  MONEY_MARKET = 'MONEY_MARKET'
}

export enum AccountStatus {
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  CLOSED = 'CLOSED'
}

export class AccountDTO {
  constructor(data: Partial<AccountDTO>) {
    Object.assign(this, data);
  }

  id?: string | null;
  accountNumber?: string | null;
  customerId?: string | null;
  accountType?: AccountType | null;
  balance?: number | null;
  accountStatus?: AccountStatus | null;
  minimumBalance?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  version?: number | null;
}
