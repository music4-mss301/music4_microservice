import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Music, Lock, User, Eye, EyeOff } from 'lucide-react';
import axiosClient from '../app/axios/axiosClient';
import { useAuthStore } from '../features/auth/useAuthStore';

// Cụm style đặc trị để chống trình duyệt tự tô màu nền trắng khi Autofill
const autofillStyle = {
    WebkitBoxShadow: '0 0 0px 1000px #1e1e1e inset',
    WebkitTextFillColor: 'white'
};

const LoginPage = () => {
    const navigate = useNavigate();
    const { loginSuccess, error, setAuthError, isLoading, setLoading } = useAuthStore();

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setAuthError(null);

        try {
            const response = await axiosClient.post('/auth/login', { username, password });

            // Trích xuất các trường cơ bản từ phản hồi Backend
            const { accessToken, refreshToken, id, img } = response.data;

            // --- XỬ LÝ TRÍCH XUẤT AN TOÀN FRONTEND: GIẢI MÃ TOKEN ---
            const base64Url = accessToken.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));

            const decodedToken = JSON.parse(jsonPayload);

            // Sử dụng các biến cục bộ thông thường, loại bỏ hoàn toàn từ khóa khai báo trùng lặp
            const finalUsername = decodedToken.sub || 'Listener';

            let rawRole = decodedToken.role || decodedToken.roles || decodedToken.authorities || 'listener';
            let finalRole = 'listener';
            if (Array.isArray(rawRole)) {
                finalRole = rawRole[0]?.replace('ROLE_', '').toLowerCase() || 'listener';
            } else if (typeof rawRole === 'string') {
                finalRole = rawRole.replace('ROLE_', '').toLowerCase();
            }
            if (finalRole === 'user') finalRole = 'listener';

            // Đồng bộ trạng thái kiên cố vào Zustand Store
            loginSuccess(accessToken, refreshToken, id, finalUsername, img, finalRole);
            navigate('/');
        } catch (err) {
            const errorMsg = err.response?.data?.message || 'Tài khoản hoặc mật khẩu không chính xác!';
            setAuthError(errorMsg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-black flex items-center justify-center p-4 font-sans text-gray-100">
            <div className="w-full max-w-md bg-[#121212] border border-[#282828] p-8 rounded-2xl shadow-xl transition-all hover:border-[#3e3e3e]">

                <div className="flex flex-col items-center mb-10">
                    <div className="w-14 h-14 bg-blue-600 rounded-full flex items-center justify-center mb-4">
                        <Music className="text-white" size={26} />
                    </div>
                    <h2 className="text-3xl font-extrabold text-white tracking-wider">
                        MUSIC<span className="text-blue-500">4</span>
                    </h2>
                    <p className="text-xs text-[#a7a7a7] mt-1 uppercase tracking-widest">Hệ thống đăng nhập</p>
                </div>

                {error && (
                    <div className="mb-6 p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm text-center">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-5">
                    <div>
                        <label className="block text-xs font-semibold text-[#a7a7a7] uppercase tracking-wider mb-2">Tên tài khoản</label>
                        <div className="relative flex items-center bg-[#1e1e1e] border border-[#282828] rounded-full px-5 py-3.5 focus-within:border-blue-500 transition-all">
                            <User className="text-[#a7a7a7] mr-3" size={18} />
                            <input
                                type="text"
                                required
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                placeholder="Nhập username của bạn"
                                className="bg-transparent border-none outline-none text-sm text-white w-full placeholder-[#535353]"
                                style={autofillStyle}
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-xs font-semibold text-[#a7a7a7] uppercase tracking-wider mb-2">Mật khẩu</label>
                        <div className="relative flex items-center bg-[#1e1e1e] border border-[#282828] rounded-full px-5 py-3.5 focus-within:border-blue-500 transition-all">
                            <Lock className="text-[#a7a7a7] mr-3" size={18} />
                            <input
                                type={showPassword ? "text" : "password"}
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                className="bg-transparent border-none outline-none text-sm text-white w-full placeholder-[#535353]"
                                style={autofillStyle}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="text-[#a7a7a7] hover:text-white transition-colors bg-transparent border-none cursor-pointer"
                            >
                                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                            </button>
                        </div>
                    </div>

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-3.5 rounded-full transition-all duration-300 transform active:scale-98 flex items-center justify-center text-sm tracking-wider cursor-pointer border-none"
                    >
                        {isLoading ? (
                            <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        ) : "ĐĂNG NHẬP"}
                    </button>
                </form>

                <div className="mt-8 text-center text-xs text-[#a7a7a7] border-t border-[#282828] pt-6">
                    Chưa có tài khoản?{' '}
                    <button
                        onClick={() => navigate('/register')}
                        className="text-white font-bold hover:text-blue-400 hover:underline bg-transparent border-none p-0 cursor-pointer transition-colors"
                    >
                        Đăng ký ngay
                    </button>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;