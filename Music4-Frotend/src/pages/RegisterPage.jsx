import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Music, Lock, User, Mail, Shield, AlertCircle, CheckCircle2 } from 'lucide-react';
import axiosClient from '../app/axios/axiosClient';

// Cụm style đặc trị để chống trình duyệt tự tô màu nền trắng khi Autofill
const autofillStyle = {
    WebkitBoxShadow: '0 0 0px 1000px #1e1e1e inset',
    WebkitTextFillColor: 'white'
};

const RegisterPage = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        username: '',
        password: '',
        role: 'listener'
    });

    const [status, setStatus] = useState({ success: null, error: null, loading: false });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setStatus({ success: null, error: null, loading: true });

        if (formData.password.length < 6) {
            setStatus({
                success: null,
                error: "Mật khẩu quá ngắn! Phải chứa ít nhất 6 ký tự.",
                loading: false
            });
            return;
        }

        // Bọc dữ liệu thành dạng Multipart Form-Data để gửi xuống BE
        const dataToSend = new FormData();
        dataToSend.append('name', formData.name);
        dataToSend.append('email', formData.email);
        dataToSend.append('username', formData.username);
        dataToSend.append('password', formData.password);

        try {
            const response = await axiosClient.post(`/auth/register?role=${formData.role}`, dataToSend, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });

            setStatus({ success: response.data.message || "Đăng ký tài khoản thành công!", error: null, loading: false });
            setTimeout(() => navigate('/login'), 2000);
        } catch (err) {
            if (!err.response) {
                setStatus({ success: null, error: "Không thể kết nối đến máy chủ!", loading: false });
                return;
            }

            const statusCode = err.response.status;
            const backendMessage = err.response.data?.message;
            let userFriendlyError = "";

            switch (statusCode) {
                case 400:
                    userFriendlyError = `[Lỗi 400]: ${backendMessage || "Dữ liệu đăng ký không hợp lệ."}`;
                    break;
                case 409:
                    userFriendlyError = `[Lỗi 409]: ${backendMessage || "Username hoặc địa chỉ Email đã được sử dụng!"}`;
                    break;
                case 500:
                    userFriendlyError = `[Lỗi 500]: ${backendMessage || "Máy chủ gặp sự cố DB."}`;
                    break;
                default:
                    userFriendlyError = `[Lỗi ${statusCode}]: ${backendMessage || "Đăng ký không thành công!"}`;
            }

            setStatus({ success: null, error: userFriendlyError, loading: false });
        }
    };

    return (
        <div className="min-h-screen bg-black flex items-center justify-center p-4 text-gray-100 font-sans relative overflow-hidden">
            <div className="w-full max-w-md bg-[#121212] border border-[#282828] p-8 rounded-2xl shadow-xl transition-all hover:border-[#3e3e3e]">
                <div className="text-center mb-6">
                    <div className="w-14 h-14 bg-blue-600 rounded-full flex items-center justify-center mx-auto mb-3">
                        <Music className="text-white" size={26} />
                    </div>
                    <h2 className="text-2xl font-bold text-white">Tạo tài khoản mới</h2>
                    <p className="text-xs text-[#a7a7a7] mt-1">Tham gia cùng cộng đồng âm nhạc</p>
                </div>

                {status.error && (
                    <div className="mb-5 p-3.5 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm flex items-center gap-2.5">
                        <AlertCircle size={18} className="shrink-0" />
                        <span className="font-medium">{status.error}</span>
                    </div>
                )}

                {status.success && (
                    <div className="mb-5 p-3.5 bg-green-500/10 border border-green-500/30 rounded-xl text-green-400 text-sm flex items-center gap-2.5">
                        <CheckCircle2 size={18} className="shrink-0" />
                        <span className="font-medium">{status.success}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-xs font-semibold text-[#a7a7a7] uppercase tracking-wider mb-1.5">Họ và Tên</label>
                        <div className="flex items-center bg-[#1e1e1e] border border-[#282828] rounded-full px-5 py-3 focus-within:border-blue-500 transition-all">
                            <User className="text-[#a7a7a7] mr-3" size={16} />
                            <input
                                type="text"
                                required
                                placeholder="Nguyễn Văn A"
                                className="bg-transparent text-sm text-white w-full outline-none placeholder-[#535353]"
                                style={autofillStyle}
                                onChange={e => setFormData({...formData, name: e.target.value})}
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-xs font-semibold text-[#a7a7a7] uppercase tracking-wider mb-1.5">Địa chỉ Email</label>
                        <div className="flex items-center bg-[#1e1e1e] border border-[#282828] rounded-full px-5 py-3 focus-within:border-blue-500 transition-all">
                            <Mail className="text-[#a7a7a7] mr-3" size={16} />
                            <input
                                type="email"
                                required
                                placeholder="abc@domain.com"
                                className="bg-transparent text-sm text-white w-full outline-none placeholder-[#535353]"
                                style={autofillStyle}
                                onChange={e => setFormData({...formData, email: e.target.value})}
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-xs font-semibold text-[#a7a7a7] uppercase tracking-wider mb-1.5">Tên tài khoản</label>
                        <div className="flex items-center bg-[#1e1e1e] border border-[#282828] rounded-full px-5 py-3 focus-within:border-blue-500 transition-all">
                            <User className="text-[#a7a7a7] mr-3" size={16} />
                            <input
                                type="text"
                                required
                                placeholder="username123"
                                className="bg-transparent text-sm text-white w-full outline-none placeholder-[#535353]"
                                style={autofillStyle}
                                onChange={e => setFormData({...formData, username: e.target.value})}
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-xs font-semibold text-[#a7a7a7] uppercase tracking-wider mb-1.5">Mật khẩu</label>
                        <div className="flex items-center bg-[#1e1e1e] border border-[#282828] rounded-full px-5 py-3 focus-within:border-blue-500 transition-all">
                            <Lock className="text-[#a7a7a7] mr-3" size={16} />
                            <input
                                type="password"
                                required
                                placeholder="Tối thiểu 6 ký tự"
                                className="bg-transparent text-sm text-white w-full outline-none placeholder-[#535353]"
                                style={autofillStyle}
                                onChange={e => setFormData({...formData, password: e.target.value})}
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-xs font-semibold text-[#a7a7a7] uppercase tracking-wider mb-1.5">Vai trò tham gia</label>
                        <div className="flex items-center bg-[#1e1e1e] border border-[#282828] rounded-full px-5 py-3 focus-within:border-blue-500 transition-all">
                            <Shield className="text-[#a7a7a7] mr-3" size={16} />
                            <select className="bg-transparent text-sm text-white w-full outline-none cursor-pointer" value={formData.role} onChange={e => setFormData({...formData, role: e.target.value})}>
                                <option value="listener" className="bg-[#121212] text-white">Người nghe nhạc (Listener)</option>
                                <option value="artist" className="bg-[#121212] text-white">Nghệ sĩ sáng tác (Artist)</option>
                            </select>
                        </div>
                    </div>

                    <button type="submit" disabled={status.loading} className="w-full mt-2 bg-blue-600 hover:bg-blue-500 text-white font-bold py-3.5 rounded-full text-sm tracking-wider transition-all flex items-center justify-center transform active:scale-98 border-none cursor-pointer">
                        {status.loading ? ( <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div> ) : "ĐĂNG KÝ TÀI KHOẢN"}
                    </button>
                </form>

                <div className="mt-6 text-center text-xs text-[#a7a7a7] border-t border-[#282828] pt-6">
                    Đã có tài khoản?{' '}
                    <button onClick={() => navigate('/login')} className="text-white font-bold hover:text-blue-400 hover:underline bg-transparent border-none p-0 cursor-pointer">
                        Đăng nhập ngay
                    </button>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;