import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, User as UserIcon } from 'lucide-react';
import axiosClient from '../app/axios/axiosClient';
import MusicImage from "../layouts/components/MusicImage.jsx";

const AllArtistsPage = () => {
    const navigate = useNavigate();
    const [artists, setArtists] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchAllArtists = async () => {
            try {
                const res = await axiosClient.get('/artists');
                setArtists(res.data || []);
            } catch (error) {
                console.error("Lỗi tải danh sách nghệ sĩ: ", error);
            } finally {
                setIsLoading(false);
            }
        };
        fetchAllArtists();
    }, []);

    if (isLoading) {
        return (
            <div className="p-6 bg-[#121212] min-h-screen flex items-center justify-center font-sans">
                <div className="w-10 h-10 border-4 border-[#282828] border-t-blue-500 rounded-full animate-spin"></div>
            </div>
        );
    }

    return (
        <div className="p-6 pb-32 bg-[#121212] min-h-screen font-sans text-gray-100">
            {/* Nút quay lại trang chủ phẳng */}
            <button
                onClick={() => navigate('/')}
                className="flex items-center gap-2 text-sm font-bold text-[#a7a7a7] hover:text-white mb-6 bg-transparent border-none cursor-pointer transition-colors"
            >
                <ArrowLeft size={18} /> Quay lại trang chủ
            </button>

            <h2 className="text-3xl font-extrabold text-white mb-8 tracking-tight">Nghệ sĩ phổ biến</h2>

            {/* Lưới hiển thị danh sách nghệ sĩ */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                {artists.map((artist) => (
                    <div
                        key={artist.id}
                        onClick={() => navigate(`/artist/${artist.id}`)}
                        className="bg-[#181818] p-5 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e] text-center"
                    >
                        {/* Khung hình tròn chuẩn Spotify */}
                        <div className="w-28 h-28 md:w-32 md:h-32 mx-auto mb-4 rounded-full overflow-hidden border border-[#282828] relative bg-[#282828] shadow-md">
                            <MusicImage
                                src={artist.img}
                                type='artist'
                                alt={artist.name}
                                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 object-top"
                            />
                            <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                                <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center shadow-md transform translate-y-2 group-hover:translate-y-0 transition-all duration-300">
                                    <UserIcon size={18} className="text-white" />
                                </div>
                            </div>
                        </div>
                        <h4 className="font-bold text-white truncate text-sm mb-1 group-hover:text-blue-400 transition-colors">{artist.name}</h4>
                        <p className="text-[11px] text-[#a7a7a7] font-medium tracking-wider uppercase">Artist</p>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default AllArtistsPage;