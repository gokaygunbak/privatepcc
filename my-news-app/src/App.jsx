// src/App.jsx
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';     // Login sayfan
import NewsPage from './pages/NewsPage'; // Az önce oluşturduğumuz Haberler sayfan
import Register from './pages/Register';

import Onboarding from './pages/Onboarding';
import MyFeed from './pages/MyFeed';
import TrendsPage from './pages/TrendsPage';
import ProfilePage from './pages/ProfilePage';
import SavedContentsPage from './pages/SavedContentsPage';
import AdminPage from './pages/AdminPage';

function App() {
  return (
    <Router>
      <Routes>
        {/* Ana sayfa olarak Login açılır */}
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Giriş başarılı olursa buraya yönlendirilir */}
        <Route path="/news" element={<NewsPage />} />
        <Route path="/trends" element={<TrendsPage />} />
        <Route path="/my-feed" element={<MyFeed />} />
        <Route path="/saved" element={<SavedContentsPage />} />
        <Route path="/onboarding" element={<Onboarding />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Routes>
    </Router>
  );
}

export default App;