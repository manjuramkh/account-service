import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router';
import axios from 'axios';
import { handleServerError } from 'app/common/utils';
import useDocumentTitle from 'app/common/use-document-title';
import { AccountDTO } from 'app/account/account-model';

export default function AccountView() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [account, setAccount] = useState<AccountDTO | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useDocumentTitle(t('account.view.headline'));

  useEffect(() => {
    const fetchAccount = async () => {
      try {
        const response = await axios.get(`/api/v1/accounts/${id}`);
        setAccount(new AccountDTO(response.data));
      } catch (error: any) {
        handleServerError(error, navigate);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchAccount();
    }
  }, [id, navigate]);

  const formatCurrency = (amount: number | null | undefined) => {
    if (amount === null || amount === undefined) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  if (loading) {
    return (
      <div className="text-center">
        <div className="spinner-border" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  if (!account) {
    return <div className="alert alert-danger">{t('account.notFound')}</div>;
  }

  return (
    <>
      <h1>{t('account.view.headline')}</h1>
      <div className="card">
        <div className="card-body">
          <div className="row mb-3">
            <div className="col-md-6">
              <h5 className="card-title">{t('account.accountNumber.label')}</h5>
              <p className="card-text">
                <strong>{account.accountNumber}</strong>
              </p>
            </div>
            <div className="col-md-6">
              <h5 className="card-title">{t('account.type.label')}</h5>
              <p className="card-text">{account.accountType}</p>
            </div>
          </div>

          <div className="row mb-3">
            <div className="col-md-6">
              <h5 className="card-title">{t('account.balance.label')}</h5>
              <p className="card-text">
                <strong>{formatCurrency(account.balance)}</strong>
              </p>
            </div>
            <div className="col-md-6">
              <h5 className="card-title">{t('account.status.label')}</h5>
              <p className="card-text">
                <span className={`badge ${
                  account.accountStatus === 'ACTIVE'
                    ? 'bg-success'
                    : account.accountStatus === 'SUSPENDED'
                    ? 'bg-warning'
                    : 'bg-danger'
                }`}>
                  {account.accountStatus}
                </span>
              </p>
            </div>
          </div>

          <div className="row mb-3">
            <div className="col-md-6">
              <h5 className="card-title">{t('account.minimumBalance.label')}</h5>
              <p className="card-text">{formatCurrency(account.minimumBalance)}</p>
            </div>
            <div className="col-md-6">
              <h5 className="card-title">{t('account.createdAt.label')}</h5>
              <p className="card-text">
                {new Date(account.createdAt || '').toLocaleString()}
              </p>
            </div>
          </div>

          <div className="row">
            <div className="col-md-12">
              <button onClick={() => navigate('/accounts')} className="btn btn-secondary">
                {t('back')}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
