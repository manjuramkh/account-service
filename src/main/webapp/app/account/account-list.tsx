import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { handleServerError } from 'app/common/utils';
import { AccountDTO } from 'app/account/account-model';
import axios from 'axios';
import useDocumentTitle from 'app/common/use-document-title';

export default function AccountList() {
  const { t } = useTranslation();
  useDocumentTitle(t('account.list.headline'));

  const [accounts, setAccounts] = useState<AccountDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const navigate = useNavigate();

  const getAllAccounts = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/v1/accounts');
      setAccounts(response.data);
    } catch (error: any) {
      handleServerError(error, navigate);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    getAllAccounts();
  }, []);

  const formatCurrency = (amount: number | null | undefined) => {
    if (amount === null || amount === undefined) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const getStatusBadgeClass = (status: string | null | undefined) => {
    switch (status) {
      case 'ACTIVE':
        return 'badge bg-success';
      case 'SUSPENDED':
        return 'badge bg-warning';
      case 'CLOSED':
        return 'badge bg-danger';
      default:
        return 'badge bg-secondary';
    }
  };

  return (
    <>
      <div className="d-flex flex-wrap mb-4">
        <h1 className="flex-grow-1">{t('account.list.headline')}</h1>
        <div>
          <Link to="/accounts/add" className="btn btn-primary ms-2">
            {t('account.list.createNew')}
          </Link>
        </div>
      </div>

      {loading ? (
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      ) : !accounts || accounts.length === 0 ? (
        <div className="alert alert-info">{t('account.list.empty')}</div>
      ) : (
        <div className="table-responsive">
          <table className="table table-striped table-hover align-middle">
            <thead>
              <tr>
                <th scope="col">{t('account.id.label')}</th>
                <th scope="col">{t('account.accountNumber.label')}</th>
                <th scope="col">{t('account.type.label')}</th>
                <th scope="col">{t('account.balance.label')}</th>
                <th scope="col">{t('account.status.label')}</th>
                <th scope="col">{t('account.createdAt.label')}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr key={account.id}>
                  <td>{account.id}</td>
                  <td>
                    <strong>{account.accountNumber}</strong>
                  </td>
                  <td>{account.accountType}</td>
                  <td>{formatCurrency(account.balance)}</td>
                  <td>
                    <span className={getStatusBadgeClass(account.accountStatus)}>
                      {account.accountStatus}
                    </span>
                  </td>
                  <td>{new Date(account.createdAt || '').toLocaleDateString()}</td>
                  <td>
                    <div className="float-end text-nowrap">
                      <Link
                        to={'/accounts/view/' + account.id}
                        className="btn btn-sm btn-info"
                      >
                        {t('account.list.view')}
                      </Link>
                      <span> </span>
                      <Link
                        to={'/accounts/transactions/' + account.id}
                        className="btn btn-sm btn-secondary"
                      >
                        {t('account.list.transactions')}
                      </Link>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
