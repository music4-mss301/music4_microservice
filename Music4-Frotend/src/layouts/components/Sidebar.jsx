import React from 'react';
import { Home, Search, Library, PlusSquare, Heart, Music } from 'lucide-react';
import { Link } from 'react-router-dom';

const Sidebar = ({ playlists = [] }) => {
    return (
        <div className="w-64 bg-black p-6 flex flex-col h-full border-r border-[#282828] hidden md:flex font-sans text-gray-400">
            <Link to="/" className="flex items-center gap-3 mb-8 cursor-pointer group">
                <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center transition-all duration-300">
                    <Music className="text-white" size={20} />
                </div>
                <h1 className="text-2xl font-bold tracking-wider text-white">
                    MUSIC<span className="text-blue-500">4</span>
                </h1>
            </Link>

            <nav className="flex flex-col gap-4 font-medium">
                <Link to="/" className="flex items-center gap-4 hover:text-white transition-all">
                    <Home size={22} /> Trang chủ
                </Link>
                <Link to="/library" className="flex items-center gap-4 hover:text-white transition-all">
                    <Library size={22} /> Thư viện
                </Link>
            </nav>

            <div className="mt-8 pt-6 border-t border-[#282828] flex flex-col gap-4 font-medium">
                <button className="flex items-center gap-4 hover:text-white transition-all group w-full text-left bg-transparent border-none outline-none cursor-pointer p-0">
                    <div className="bg-[#282828] group-hover:bg-[#3e3e3e] p-1 rounded-sm text-white transition-colors">
                        <PlusSquare size={20} />
                    </div>
                    Tạo Playlist
                </button>
                <Link to="/favorites" className="flex items-center gap-4 hover:text-white transition-all group">
                    <div className="bg-[#282828] p-1 rounded-sm text-white transition-all">
                        <Heart size={20} />
                    </div>
                    Bài hát đã thích
                </Link>
            </div>

            <div className="mt-6 flex-1 overflow-y-auto custom-scrollbar">
                <ul className="flex flex-col gap-3 text-sm text-[#a7a7a7]">
                    {playlists.map((pl) => (
                        <li key={pl.id} className="hover:text-blue-500 cursor-pointer transition-colors truncate">
                            <Link to={`/playlist/${pl.id}`}>{pl.name}</Link>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    );
};

export default Sidebar;