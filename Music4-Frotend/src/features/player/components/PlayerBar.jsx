import React from 'react';
import {
    Heart, Play, Pause, SkipBack, SkipForward, Volume2,
    Mic2, MonitorSpeaker, Repeat, Shuffle, Maximize2
} from 'lucide-react';

// Props nhận từ Zustand (Global State) hoặc Parent Component
const PlayerBar = ({ currentTrack, isPlaying, onTogglePlay, onNext, onPrev }) => {
    return (
        <div className="h-24 bg-gray-950 border-t border-gray-800 flex items-center justify-between px-4 sm:px-6 relative z-50 shadow-[0_-5px_20px_rgba(0,0,0,0.5)]">
            <div className="absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-cyan-500 to-transparent opacity-50"></div>

            {/* Thông tin bài hát (Render nếu có currentTrack) */}
            <div className="flex items-center gap-4 w-1/3">
                {currentTrack ? (
                    <>
                        <div className="w-14 h-14 bg-gray-800 rounded-md overflow-hidden relative group cursor-pointer shadow-[0_0_10px_rgba(0,0,0,0.5)]">
                            <img src={currentTrack.coverUrl} alt="Cover" className="w-full h-full object-cover" />
                        </div>
                        <div className="hidden sm:block">
                            <h4 className="text-sm font-semibold text-white hover:underline cursor-pointer">{currentTrack.title}</h4>
                            <p className="text-xs text-gray-400 hover:underline cursor-pointer">{currentTrack.artist}</p>
                        </div>
                        <button className="ml-2">
                            <Heart size={20} className="text-gray-400 hover:text-cyan-400 hover:drop-shadow-[0_0_8px_rgba(34,211,238,0.8)] transition-all" />
                        </button>
                    </>
                ) : (
                    <div className="text-xs text-gray-500">Chưa có bài hát nào được phát</div>
                )}
            </div>

            {/* Cụm nút điều khiển */}
            <div className="flex flex-col items-center justify-center w-1/3 max-w-md">
                <div className="flex items-center gap-6 mb-2">
                    <Shuffle size={18} className="text-gray-400 hover:text-white cursor-pointer hidden sm:block" />
                    <SkipBack size={24} className="text-gray-300 hover:text-white cursor-pointer" onClick={onPrev} />

                    <button
                        onClick={onTogglePlay}
                        disabled={!currentTrack}
                        className={`w-10 h-10 flex items-center justify-center rounded-full transition-all ${currentTrack ? 'bg-white text-black hover:scale-105 hover:bg-cyan-400 hover:shadow-[0_0_15px_rgba(34,211,238,0.8)]' : 'bg-gray-700 text-gray-500 cursor-not-allowed'}`}
                    >
                        {isPlaying ? <Pause size={20} fill="currentColor" /> : <Play size={20} fill="currentColor" className="ml-1" />}
                    </button>

                    <SkipForward size={24} className="text-gray-300 hover:text-white cursor-pointer" onClick={onNext} />
                    <Repeat size={18} className="text-gray-400 hover:text-white cursor-pointer hidden sm:block" />
                </div>

                {/* Thanh tiến trình */}
                <div className="w-full flex items-center gap-2 text-xs text-gray-400">
                    <span>0:00</span> {/* Sẽ được cập nhật động bởi ref của thẻ <audio> */}
                    <div className="flex-1 h-1.5 bg-gray-700 rounded-full cursor-pointer group relative">
                        <div className="w-0 h-full bg-cyan-400 rounded-full group-hover:bg-cyan-300 shadow-[0_0_8px_rgba(34,211,238,0.6)]"></div>
                    </div>
                    <span>0:00</span>
                </div>
            </div>

            {/* Âm lượng & Mở rộng */}
            <div className="flex items-center justify-end gap-4 w-1/3 text-gray-400 hidden md:flex">
                <Mic2 size={18} className="hover:text-white cursor-pointer" />
                <MonitorSpeaker size={18} className="hover:text-white cursor-pointer" />
                <div className="flex items-center gap-2 w-28 group cursor-pointer">
                    <Volume2 size={18} className="hover:text-white" />
                    <div className="flex-1 h-1 bg-gray-700 rounded-full">
                        <div className="w-2/3 h-full bg-gray-400 group-hover:bg-cyan-400 rounded-full transition-colors"></div>
                    </div>
                </div>
                <Maximize2 size={18} className="hover:text-white cursor-pointer" />
            </div>
        </div>
    );
};

export default PlayerBar;