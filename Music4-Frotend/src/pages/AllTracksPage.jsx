import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Play, ArrowLeft } from 'lucide-react';
import axiosClient from '../app/axios/axiosClient';
import { usePlayerStore } from '../features/player/usePlayerStore';
import MusicImage from "../layouts/components/MusicImage.jsx";
const AllTracksPage = () => {
    const navigate = useNavigate();

    const playTrack = usePlayerStore((state) => state.playTrack);
    const [tracks, setTracks] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchAllTracks = async () => {
            try {
                const res = await axiosClient.get('/tracks');
                setTracks(res.data || []);
            } catch (error) {
                console.error("Lỗi tải danh sách bài hát: ", error);
            } finally {
                setIsLoading(false);
            }
        };
        fetchAllTracks();
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

            <h2 className="text-3xl font-extrabold text-white mb-8 tracking-tight">Tất cả bài hát hệ thống</h2>

            {/* Lưới hiển thị danh sách bài hát */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                {tracks.map((track) => (
                    <div
                        key={track.id}
                        className="bg-[#181818] p-4 rounded-xl hover:bg-[#282828] transition-all duration-300 group cursor-pointer border border-transparent hover:border-[#3e3e3e]"
                    >
                        <div className="relative aspect-square mb-4 rounded-md overflow-hidden bg-[#282828]">
                            <MusicImage
                                src={track.img}
                                typr='track'
                                alt={track.name}
                                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                            />
                            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity duration-300">
                                <button
                                    onClick={(e) => {
                                        e.stopPropagation(); // Chặn sự kiện click lan ra thẻ cha
                                        playTrack(track, tracks); // Phát bài này và đưa toàn bộ mảng kết quả tìm kiếm 'tracks' vào hàng đợi
                                    }}
                                    className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center text-white border-none cursor-pointer shadow-md"
                                >
                                    <Play size={18} fill="currentColor" className="text-black ml-0.5" />
                                </button>
                            </div>
                        </div>
                        <h4 className="font-bold text-white truncate text-sm mb-1">{track.name}</h4>
                        <p className="text-xs text-[#a7a7a7] truncate flex gap-1 items-center">
                            {track.artists && track.artists.length > 0 ? (
                                track.artists.map((artist, idx) => (
                                    <span key={artist.id}>
                            <span
                                onClick={(e) => {
                                    e.stopPropagation(); // Ngăn chặn sự kiện click lan ra thẻ cha (làm phát nhạc ngoài ý muốn)
                                    navigate(`/artist/${artist.id}`); // Chuyển hướng đến trang nghệ sĩ bằng ID
                                }}
                                className="hover:text-blue-500 hover:underline cursor-pointer transition-colors text-gray-400 font-medium"
                            >
                                {artist.name}
                            </span>
                                        {/* Nếu bài hát có nhiều nghệ sĩ, thêm dấu phẩy ngăn cách ở giữa */}
                                        {idx < track.artists.length - 1 && ", "}
                        </span>
                                ))
                            ) : (
                                "Nghệ sĩ hệ thống"
                            )}
                        </p>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default AllTracksPage;