import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../useAuthStore';

const ProtectedRoute = () => {
    const { isAuthenticated } = useAuthStore();

    // Nếu isAuthenticated = false (Chưa đăng nhập), ép buộc chuyển hướng về /login
    return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};

export default ProtectedRoute;