import React from 'react';
import Sidebar from './components/Sidebar';
import Navbar from './components/Navbar';
import { Outlet } from 'react-router-dom';
import MusicPlayer from "./components/MusicPlayer.jsx";

const MainLayout = () => {
    return (
        <div className="flex h-screen w-screen bg-black text-white font-sans overflow-hidden">
            {/* 1. Thanh Sidebar cố định bên trái */}
            <Sidebar />

            {/* 2. Khối nội dung bên phải bao gồm Navbar + Trang nội dung */}
            <div className="flex-1 flex flex-col h-full overflow-hidden bg-[#121212]">
                {/* Luôn hiển thị Navbar phẳng ở trên cùng */}
                <Navbar />

                {/* Phân vùng cuộn nội dung các trang (HomePage, SearchPage...) */}
                <main className="flex-1 overflow-y-auto custom-scrollbar">
                    <Outlet />
                </main>
            </div>
            <MusicPlayer />
        </div>
    );
};

export default MainLayout;