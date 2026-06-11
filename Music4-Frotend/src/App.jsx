import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import HomePage from './pages/HomePage';
import RegisterPage from "./pages/RegisterPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import AllTracksPage from "./pages/AllTracksPage.jsx";
import AllArtistsPage from "./pages/AllArtistsPage.jsx";
import SearchPage from "./pages/SearchPage.jsx";

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<MainLayout />}>
                    <Route index element={<HomePage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
                    <Route path="tracks" element={<AllTracksPage />} />
                    <Route path="artists" element={<AllArtistsPage />} />
                    <Route path="/search" element={<SearchPage />} />
                    {/* Thêm các Route khác vào đây, ví dụ: */}
                    {/* <Route path="search" element={<SearchPage />} /> */}
                </Route>
            </Routes>
        </BrowserRouter>
    );
}

export default App;