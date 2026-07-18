import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import axios from 'axios';
import { handleServerError } from 'app/common/utils';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import { AccountType } from 'app/account/account-model';

interface AccountFormData {
  customerId: string;
  accountType: string;
  initialBalance: number;
}

const schema = yup.object().shape({
  customerId: yup.string().required('Customer ID is required'),
  accountType: yup.string().required('Account Type is required'),
  initialBalance: yup
    .number()
    .positive('Initial balance must be positive')
    .required('Initial balance is required')
});

export default function AccountAdd() {
  const { t } = useTranslation();
  useDocumentTitle(t('account.add.headline'));

  const navigate = useNavigate();
  const [loading, setLoading] = useState<boolean>(false);

  const useFormResult = useForm<AccountFormData>({
    resolver: yupResolver(schema),
    defaultValues: {
      customerId: '',
      accountType: AccountType.SAVINGS,
      initialBalance: 0
    }
  });

  const { handleSubmit } = useFormResult;

  const onSubmit = async (data: AccountFormData) => {
    setLoading(true);
    try {
      await axios.post('/api/v1/accounts', {
        customerId: data.customerId,
        accountType: data.accountType,
        initialBalance: data.initialBalance
      });
      navigate('/accounts', {
        state: {
          msgInfo: t('account.add.success')
        }
      });
    } catch (error: any) {
      handleServerError(error, navigate);
    } finally {
      setLoading(false);
    }
  };

  const accountTypeOptions: Record<string, string> = {
    SAVINGS: 'Savings Account',
    CHECKING: 'Checking Account',
    MONEY_MARKET: 'Money Market Account'
  };

  return (
    <>
      <h1>{t('account.add.headline')}</h1>
      <form onSubmit={handleSubmit(onSubmit)}>
        <InputRow
          useFormResult={useFormResult}
          object="account"
          field="customerId"
          type="text"
          required={true}
        />

        <InputRow
          useFormResult={useFormResult}
          object="account"
          field="accountType"
          type="select"
          required={true}
          options={accountTypeOptions}
        />

        <InputRow
          useFormResult={useFormResult}
          object="account"
          field="initialBalance"
          type="number"
          required={true}
          inputClass="form-control"
        />

        <div className="mb-3">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                ></span>
                {t('loading')}
              </>
            ) : (
              t('account.add.submitButton')
            )}
          </button>
        </div>
      </form>
    </>
  );
}
