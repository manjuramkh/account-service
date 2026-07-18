import React from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';
import App from "./app";
import Home from './home/home';
import Error from './error/error';
import AccountList from './account/account-list';
import AccountAdd from './account/account-add';
import AccountView from './account/account-view';


export default function AppRoutes() {
  const router = createBrowserRouter([
    {
      element: <App />,
      children: [
        { path: '', element: <Home /> },
        { path: 'accounts', element: <AccountList /> },
        { path: 'accounts/add', element: <AccountAdd /> },
        { path: 'accounts/view/:id', element: <AccountView /> },
        { path: 'error', element: <Error /> },
        { path: '*', element: <Error /> }
      ]
    }
  ]);

  return (
    <RouterProvider router={router} />
  );
}
