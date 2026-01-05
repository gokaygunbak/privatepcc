import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import {
    Favorite as FavoriteIcon,
    Bookmark as BookmarkIcon,
    FavoriteBorder, BookmarkBorder,
    Report as ReportIcon,
    Link as LinkIcon,
    SentimentDissatisfied
} from '@mui/icons-material';
import {
    Typography, Button, Container, Grid, Card, CardContent,
    Chip, Box, IconButton, Skeleton, Tooltip, Alert, Menu, MenuItem
} from '@mui/material';
import AuthService from '../services/AuthService';
import MainLayout from '../components/MainLayout';

const drawerWidth = 240;

const MyFeed = () => {
    const [summaries, setSummaries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [currentIndex, setCurrentIndex] = useState(0);
    const navigate = useNavigate();

    // Interaction states
    const [likedPosts, setLikedPosts] = useState({});
    const [savedPosts, setSavedPosts] = useState({});
    const [reportedPosts, setReportedPosts] = useState({});

    // Negative Feedback Menu State
    const [anchorEl, setAnchorEl] = useState(null);
    const openMenu = Boolean(anchorEl);

    const handleMenuClick = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setAnchorEl(null);
    };

    const handleNegativeFeedback = (type) => {
        if (currentNews) {
            handleInteraction(currentNews.content?.contentId, type, currentNews.topicId);
            handleMenuClose();
        }
    };

    useEffect(() => {
        const token = AuthService.getCurrentToken();
        const userId = AuthService.getCurrentUserId();
        if (!token) {
            navigate("/");
            return;
        }
        if (!userId) {
            setError("Kullanıcı bilgisi bulunamadı.");
            setLoading(false);
            return;
        }
        fetchInitialContent(token, userId);
    }, [navigate]);

    const fetchInitialContent = async (token, userId) => {
        try {
            // İlk içeriği çek (Rastgele ve görülmemiş, EN YÜKSEK SKORLU konudan olsun)
            const response = await axios.get(`http://localhost:8080/api/interactions/feed/next-random?userId=${userId}&forceTop=true`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            if (response.status === 204 || !response.data) {
                setError("Gösterilecek yeni içerik kalmadı!");
                setLoading(false);
                return;
            }
            setSummaries([response.data]);
            setLoading(false);
        } catch (error) {
            console.error("Hata:", error);
            if (error.response && error.response.status === 401) {
                AuthService.logout();
                navigate("/");
            }
            setError("İçerik yüklenirken bir sorun oluştu.");
            setLoading(false);
        }
    };

    const fetchNextContent = async () => {
        const token = AuthService.getCurrentToken();
        const userId = AuthService.getCurrentUserId();
        try {
            const response = await axios.get(`http://localhost:8080/api/interactions/feed/next-random?userId=${userId}`, {
                headers: { Authorization: `Bearer ${token}` }
            });

            if (response.status === 204 || !response.data) {
                alert("Başka içerik kalmadı!");
                return;
            }

            // Yeni içeriği listeye ekle ve indexi ilerlet
            setSummaries(prev => [...prev, response.data]);
            setCurrentIndex(prev => prev + 1);
        } catch (error) {
            console.error("Sonraki içerik hatası:", error);
        }
    };

    const handleInteraction = async (id, type, topicId) => {
        console.log(`[Interaction] ID=${id}, Type=${type}, TopicID=${topicId}`);
        // Optimistic UI update
        if (type === 'LIKE') {
            setLikedPosts(prev => ({ ...prev, [id]: !prev[id] }));
        } else if (type === 'SAVE') {
            setSavedPosts(prev => ({ ...prev, [id]: !prev[id] }));
        } else if (type === 'REPORT') {
            if (!window.confirm("Bu içeriği şikayet etmek istediğinize emin misiniz?")) {
                return;
            }
            setReportedPosts(prev => ({ ...prev, [id]: true }));
        }

        try {
            const payload = {
                userId: AuthService.getCurrentUserId(),
                contentId: id,
                interactionType: type,
                topicId: topicId
            };
            console.log("[Interaction] Sending payload:", payload);

            await axios.post('http://localhost:8080/api/interactions/interact', payload, {
                headers: { Authorization: `Bearer ${AuthService.getCurrentToken()}` }
            });
            console.log("[Interaction] Success!");

            if (type === 'REPORT') {
                alert("Şikayetiniz admin'e iletildi. Teşekkürler!");
            }
        } catch (e) {
            console.error("Etkileşim kaydedilemedi", e);
        }
    };

    const handleNext = () => {
        // Eğer zaten hafızada sonraki eleman varsa ona geç
        if (currentIndex < summaries.length - 1) {
            setCurrentIndex(prev => prev + 1);
        } else {
            // Yoksa yenisini çek
            fetchNextContent();
        }
    };

    const handlePrevious = () => {
        if (currentIndex > 0) {
            setCurrentIndex(prev => prev - 1);
        }
    };

    // Güvenli erişim için current news
    const currentNews = summaries.length > 0 ? summaries[currentIndex] : null;

    return (
        <MainLayout title="Size Özel Bülten">
            <Container maxWidth="lg" sx={{ minHeight: '80vh', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Typography variant="h4" sx={{ mb: 4, fontWeight: 'bold', textAlign: 'center' }}>
                    Size Özel Bülten
                </Typography>

                {error && summaries.length === 0 && (
                    <Alert severity="info" sx={{ mb: 3 }}>
                        {error}
                        <Button size="small" onClick={() => navigate("/onboarding")} sx={{ ml: 2 }}>
                            İlgi Alanlarını Düzenle
                        </Button>
                    </Alert>
                )}

                {loading ? (
                    <Box sx={{ width: '100%', maxWidth: 800 }}>
                        <Skeleton variant="rectangular" height={400} sx={{ borderRadius: 4 }} />
                    </Box>
                ) : (
                    <>
                        {currentNews ? (
                            <>
                                <Box sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    gap: 2,
                                    width: '100%',
                                    position: 'relative' // Okları konumlandırmak için
                                }}>
                                    {/* Geri Butonu (Kartın Solunda) */}
                                    {currentIndex > 0 && (
                                        <IconButton
                                            onClick={handlePrevious}
                                            sx={{
                                                bgcolor: 'primary.main',
                                                color: 'white',
                                                width: 56,
                                                height: 56,
                                                boxShadow: 4,
                                                '&:hover': { bgcolor: 'primary.dark', transform: 'scale(1.1)' },
                                                transition: 'all 0.2s',
                                                display: { xs: 'none', md: 'flex' },
                                                mr: 2 // Margin right
                                            }}
                                        >
                                            <Typography variant="h4" sx={{ mb: 0.5 }}>‹</Typography>
                                        </IconButton>
                                    )}

                                    {/* Kart */}
                                    <Card sx={{
                                        width: '100%',
                                        maxWidth: 800,
                                        minHeight: 400,
                                        borderRadius: 4,
                                        boxShadow: '0 8px 40px rgba(0,0,0,0.1)',
                                        display: 'flex',
                                        flexDirection: 'column',
                                        p: 3,
                                        position: 'relative',
                                        overflow: 'visible' // Buton dışarı taşarsa diye
                                    }}>
                                        <CardContent sx={{ flex: 1 }}>
                                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                                                {currentNews.topicName && (
                                                    <Chip
                                                        label={`Kategori: ${currentNews.topicName}`}
                                                        size="small"
                                                        color="primary"
                                                        variant="outlined"
                                                    />
                                                )}
                                            </Box>

                                            <Typography component="h1" variant="h4" color="primary" gutterBottom sx={{ fontWeight: 'bold', lineHeight: 1.3 }}>
                                                {currentNews.title}
                                            </Typography>

                                            <Typography variant="h6" color="text.secondary" paragraph sx={{ lineHeight: 1.8, mt: 3, mb: 4 }}>
                                                {currentNews.summaryText}
                                            </Typography>

                                            <Box sx={{ mb: 2 }}>
                                                {currentNews.generatedTags && currentNews.generatedTags.split(',').map((tag, i) => (
                                                    <Chip key={i} label={tag.trim()} sx={{ mr: 1, mb: 1 }} />
                                                ))}
                                            </Box>
                                        </CardContent>

                                        {/* Alt Etkileşim Barı */}
                                        <Box sx={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center',
                                            mt: 2,
                                            pt: 2,
                                            borderTop: '1px solid rgba(0,0,0,0.08)'
                                        }}>
                                            <Box sx={{ display: 'flex', gap: 1 }}>
                                                <Tooltip title="Beğen">
                                                    <IconButton
                                                        onClick={() => handleInteraction(currentNews.content?.contentId, 'LIKE', currentNews.topicId)}
                                                        color={likedPosts[currentNews.content?.contentId] ? "error" : "default"}
                                                        sx={{ '& svg': { fontSize: 28 } }}
                                                    >
                                                        {likedPosts[currentNews.content?.contentId] ? <FavoriteIcon /> : <FavoriteBorder />}
                                                    </IconButton>
                                                </Tooltip>
                                                <Tooltip title="Kaydet">
                                                    <IconButton
                                                        onClick={() => handleInteraction(currentNews.content?.contentId, 'SAVE', currentNews.topicId)}
                                                        color={savedPosts[currentNews.content?.contentId] ? "secondary" : "default"}
                                                        sx={{ '& svg': { fontSize: 28 } }}
                                                    >
                                                        {savedPosts[currentNews.content?.contentId] ? <BookmarkIcon /> : <BookmarkBorder />}
                                                    </IconButton>
                                                </Tooltip>
                                                <Tooltip title="Şikayet Et">
                                                    <IconButton
                                                        onClick={() => handleInteraction(currentNews.content?.contentId, 'REPORT', currentNews.topicId)}
                                                        color={reportedPosts[currentNews.content?.contentId] ? "error" : "default"}
                                                        disabled={reportedPosts[currentNews.content?.contentId]}
                                                    >
                                                        <ReportIcon />
                                                    </IconButton>
                                                </Tooltip>

                                                {/* Negative Feedback Menu */}
                                                <Box component="span">
                                                    <Tooltip title="Daha Az Göster / İlgilenmiyorum">
                                                        <IconButton
                                                            onClick={handleMenuClick}
                                                            color="default"
                                                        >
                                                            <SentimentDissatisfied />
                                                        </IconButton>
                                                    </Tooltip>
                                                    <Menu
                                                        anchorEl={anchorEl}
                                                        open={openMenu}
                                                        onClose={handleMenuClose}
                                                    >
                                                        <MenuItem onClick={() => handleNegativeFeedback('SHOW_LESS')}>
                                                            Bu konuyu daha az göster
                                                        </MenuItem>
                                                        <MenuItem onClick={() => handleNegativeFeedback('NOT_INTERESTED')}>
                                                            Artık ilgilenmiyorum
                                                        </MenuItem>
                                                    </Menu>
                                                </Box>
                                            </Box>

                                            <Box>
                                                {/* Link */}
                                                {(currentNews.sourceUrl || currentNews.content?.url) && (
                                                    <Button
                                                        variant="outlined"
                                                        startIcon={<LinkIcon />}
                                                        href={currentNews.sourceUrl || currentNews.content?.url}
                                                        target="_blank"
                                                        onClick={() => handleInteraction(currentNews.content?.contentId, 'CLICK', currentNews.topicId)}
                                                        sx={{ borderRadius: 4 }}
                                                    >
                                                        Kaynağa Git
                                                    </Button>
                                                )}
                                            </Box>
                                        </Box>
                                    </Card>

                                    {/* İleri Butonu (Hep göster - loading değilse) */}
                                    <IconButton
                                        onClick={handleNext}
                                        sx={{
                                            bgcolor: 'primary.main',
                                            color: 'white',
                                            width: 56,
                                            height: 56,
                                            boxShadow: 4,
                                            '&:hover': { bgcolor: 'primary.dark', transform: 'scale(1.1)' },
                                            transition: 'all 0.2s',
                                            display: { xs: 'none', md: 'flex' }
                                        }}
                                    >
                                        <Typography variant="h4" sx={{ mb: 0.5 }}>›</Typography>
                                    </IconButton>
                                </Box>

                                {/* Mobil İçin Navigasyon */}
                                <Box sx={{ display: { xs: 'flex', md: 'none' }, mt: 3, width: '100%', gap: 2, justifyContent: 'center' }}>
                                    {currentIndex > 0 && (
                                        <Button
                                            variant="contained"
                                            onClick={handlePrevious}
                                            size="large"
                                            fullWidth
                                            sx={{ borderRadius: 8, py: 1.5 }}
                                        >
                                            Geri
                                        </Button>
                                    )}

                                    <Button
                                        variant="contained"
                                        onClick={handleNext}
                                        size="large"
                                        fullWidth
                                        sx={{ borderRadius: 8, py: 1.5 }}
                                    >
                                        Sıradaki
                                    </Button>
                                </Box>

                            </>
                        ) : (
                            !loading && summaries.length > 0 && (
                                <Box sx={{ textAlign: 'center', py: 5 }}>
                                    <Typography variant="h6" color="text.secondary">
                                        Tüm içerikleri okudunuz! 🎉
                                    </Typography>
                                    <Button onClick={() => window.location.reload()} sx={{ mt: 2 }}>
                                        Yenile
                                    </Button>
                                </Box>
                            )
                        )}
                    </>
                )}
            </Container>
        </MainLayout>
    );
}

export default MyFeed;
