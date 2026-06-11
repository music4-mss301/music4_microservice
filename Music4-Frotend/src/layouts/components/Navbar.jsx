import React, { useState, useRef, useEffect } from 'react';
import { Search, Bell, User, LogOut, ChevronDown } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/useAuthStore';

const Navbar = () => {
    const navigate = useNavigate();

    // Đồng bộ thêm trường dẫn 'img' đại diện từ kho lưu trữ Zustand toàn cục
    const { username, role, img, logout, isAuthenticated } = useAuthStore();
    const [dropdownOpen, setDropdownOpen] = useState(false);
    const dropdownRef = useRef(null);
    const [searchQuery, setSearchQuery] = useState('');
    const handleLogout = () => {
        logout();
        navigate('/login');
    };
    const handleSearch = (e) => {
        // Chỉ thực thi khi phím nhấn là Enter và nội dung không bị rỗng
        if (e.key === 'Enter' && searchQuery.trim() !== '') {
            // Dùng encodeURIComponent để mã hóa các ký tự đặc biệt (khoảng trắng, dấu, tiếng Việt...)
            // đảm bảo URL hợp lệ trước khi đẩy đi
            navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
        }
    };
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setDropdownOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <header className="h-20 flex items-center justify-between px-8 bg-[#09090b] border-b border-[#282828] sticky top-0 z-50 font-sans text-white">
            {/* Thanh tìm kiếm phẳng */}
            <div className="flex items-center bg-[#121212] rounded-full px-5 py-2.5 w-80 border border-[#282828] focus-within:border-blue-500 transition-all">
                <Search className="text-[#a7a7a7] mr-3" size={20} />
                <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    onKeyDown={handleSearch}
                    placeholder="Bài hát, nghệ sĩ..."
                    className="bg-transparent border-none outline-none text-sm text-white w-full placeholder-[#535353]"
                />
            </div>

            <div className="flex items-center gap-6">
                <button className="relative p-2 text-[#a7a7a7] hover:text-white transition-colors cursor-pointer bg-transparent border-none">
                    <Bell size={22} />
                    <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-blue-500 rounded-full"></span>
                </button>

                {/* KHU VỰC HÌNH AVATAR VÀ TÙY BIẾN THEO TRẠNG THÁI LOGIN */}
                <div className="relative" ref={dropdownRef}>
                    {isAuthenticated ? (
                        // TRƯỜNG HỢP 1: ĐÃ ĐĂNG NHẬP -> CHỈ HIỆN DUY NHẤT HÌNH AVATAR TRÒN GỌN GÀNG
                        <button
                            onClick={() => setDropdownOpen(!dropdownOpen)}
                            className="w-10 h-10 rounded-full overflow-hidden border-2 border-transparent hover:border-blue-500 focus:border-blue-500 transition-all duration-300 bg-[#282828] flex items-center justify-center cursor-pointer p-0 shadow-md"
                            title={username || 'User Profile'}
                        >
                            <img
                                // Nếu DB trống hoặc null, nạp ảnh chữ cái đầu thông minh tự động để làm dày UI phẳng
                                src={img && img !== 'null' ? img : `https://ui-avatars.com/api/?name=${username || 'U'}&background=0D8BFF&color=fff&bold=true&size=128`}
                                alt="User Avatar"
                                className="w-full h-full object-cover"
                            />
                        </button>
                    ) : (
                        // TRƯỜNG HỢP 2: CHƯA ĐĂNG NHẬP (KHÁCH VÃNG LAI) -> Hiện nút Guest
                        <button
                            onClick={() => setDropdownOpen(!dropdownOpen)}
                            className="flex items-center gap-2 bg-[#121212] hover:bg-[#282828] p-1.5 pr-3 rounded-full border border-[#282828] transition-colors cursor-pointer text-white"
                        >
                            <div className="w-7 h-7 bg-[#282828] rounded-full flex items-center justify-center border border-[#3e3e3e]">
                                <User size={16} className="text-[#a7a7a7]" />
                            </div>
                            <span className="text-xs font-bold max-w-[100px] truncate">Guest</span>
                            <ChevronDown size={14} className={`text-[#a7a7a7] transition-transform duration-200 ${dropdownOpen ? 'rotate-180' : ''}`} />
                        </button>
                    )}

                    {/* Xử lý hiển thị nội dung bên trong Dropdown */}
                    {dropdownOpen && (
                        <div className="absolute right-0 mt-2 w-52 bg-[#282828] border border-[#3e3e3e] rounded-lg shadow-2xl py-1 z-50 animate-fadeIn">
                            {isAuthenticated ? (
                                // NỘI DUNG ĐÃ LOGIN
                                <>
                                    <div className="px-4 py-3 border-b border-[#3e3e3e] mb-1 bg-[#1e1e1e]/50 rounded-t-lg">
                                        <p className="text-sm font-bold text-white truncate">{username}</p>
                                        <p className="text-[10px] text-[#a7a7a7] uppercase tracking-widest mt-0.5">{role === 'artist' ? 'Nghệ sĩ' : 'Người nghe'}</p>
                                    </div>

                                    <button
                                        onClick={() => { setDropdownOpen(false); navigate('/profile'); }}
                                        className="w-full text-left px-4 py-2.5 text-sm font-medium text-white hover:bg-[#3e3e3e] flex items-center gap-2.5 transition-colors bg-transparent border-none cursor-pointer"
                                    >
                                        <User size={16} className="text-[#a7a7a7]" />
                                        <span>Account</span>
                                    </button>

                                    <div className="border-t border-[#3e3e3e] my-1"></div>

                                    <button
                                        onClick={() => { setDropdownOpen(false); handleLogout(); }}
                                        className="w-full text-left px-4 py-2.5 text-sm font-medium text-red-400 hover:bg-[#3e3e3e] flex items-center gap-2.5 transition-colors bg-transparent border-none cursor-pointer"
                                    >
                                        <LogOut size={16} />
                                        <span>Log out</span>
                                    </button>
                                </>
                            ) : (
                                // NỘI DUNG CHƯA LOGIN
                                <button
                                    onClick={() => { setDropdownOpen(false); navigate('/login'); }}
                                    className="w-full text-left px-4 py-2.5 text-sm font-bold text-white hover:bg-[#3e3e3e] flex items-center gap-2.5 transition-colors bg-transparent border-none cursor-pointer"
                                >
                                    <LogOut size={16} className="text-blue-500 transform rotate-180" />
                                    <span>Login</span>
                                </button>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
};

export default Navbar;