import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import {
    Typography, Button, Container, Grid, Card, CardContent,
    Chip, Box, IconButton, Skeleton, Tooltip, Alert
} from '@mui/material';
import {
    Favorite as FavoriteIcon,
    Bookmark as BookmarkIcon,
    FavoriteBorder, BookmarkBorder,
    Report as ReportIcon
} from '@mui/icons-material';
import AuthService from '../services/AuthService';
import MainLayout from '../components/MainLayout';

const drawerWidth = 240;

function MyFeed() {
    const [summaries, setSummaries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const navigate = useNavigate();

    // Interaction states
    const [likedPosts, setLikedPosts] = useState({});
    const [savedPosts, setSavedPosts] = useState({});
    const [reportedPosts, setReportedPosts] = useState({});

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
        fetchPersonalFeed(token, userId);
    }, []);

    const fetchPersonalFeed = async (token, userId) => {
        try {
            // Kişiselleştirilmiş akış isteği
            const response = await axios.get(`http://localhost:8080/api/interactions/feed?userId=${userId}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            setSummaries(response.data);
            setLoading(false);
        } catch (error) {
            console.error("Hata:", error);
            if (error.response && error.response.status === 401) {
                AuthService.logout();
                navigate("/");
            }
            // Eğer liste boşsa 404 dönebilir veya boş liste dönebilir
            setError("Kişisel akışınız yüklenirken bir sorun oluştu veya henüz ilgi alanı seçmediniz.");
            setLoading(false);
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

    const handleLogout = () => {
        AuthService.logout();
        navigate("/");
    };

    return (
        <MainLayout title="Size Özel Bülten">
            <Container maxWidth="xl">
                <Typography variant="h4" sx={{ mb: 4, fontWeight: 'bold' }}>
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
                    <Grid container spacing={3}>
                        {[1, 2, 3].map((item) => (
                            <Grid item xs={12} key={item}>
                                <Skeleton variant="rectangular" height={150} sx={{ borderRadius: 2 }} />
                            </Grid>
                        ))}
                    </Grid>
                ) : (
                    <Grid container spacing={3}>
                        {summaries.map((news) => (
                            <Grid item xs={12} key={news.summaryId}>
                                <Card sx={{ width: '100%', display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, p: 2 }}>
                                    <CardContent sx={{ flex: 1 }}>
                                        <Typography component="div" variant="h5" color="primary" gutterBottom sx={{
                                            display: '-webkit-box',
                                            overflow: 'hidden',
                                            WebkitBoxOrient: 'vertical',
                                            WebkitLineClamp: 2,
                                        }}>
                                            {news.title}
                                        </Typography>
                                        <Typography variant="body1" color="text.secondary" paragraph sx={{
                                            display: '-webkit-box',
                                            overflow: 'hidden',
                                            WebkitBoxOrient: 'vertical',
                                            WebkitLineClamp: 3,
                                        }}>
                                            {news.summaryText}
                                        </Typography>
                                        <Box>
                                            {news.generatedTags && news.generatedTags.split(',').map((tag, i) => (
                                                <Chip key={i} label={tag.trim()} size="small" sx={{ mr: 1 }} />
                                            ))}
                                        </Box>
                                    </CardContent>
                                    <Box sx={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', borderLeft: '1px solid rgba(255,255,255,0.1)', pl: 2 }}>
                                        <Tooltip title="Beğen">
                                            <IconButton onClick={() => handleInteraction(news.content?.contentId, 'LIKE', news.topicId)} color={likedPosts[news.content?.contentId] ? "error" : "default"}>
                                                {likedPosts[news.content?.contentId] ? <FavoriteIcon /> : <FavoriteBorder />}
                                            </IconButton>
                                        </Tooltip>
                                        <Tooltip title="Kaydet">
                                            <IconButton onClick={() => handleInteraction(news.content?.contentId, 'SAVE', news.topicId)} color={savedPosts[news.content?.contentId] ? "secondary" : "default"}>
                                                {savedPosts[news.content?.contentId] ? <BookmarkIcon /> : <BookmarkBorder />}
                                            </IconButton>
                                        </Tooltip>
                                        <Tooltip title="Şikayet Et">
                                            <IconButton 
                                                onClick={() => handleInteraction(news.content?.contentId, 'REPORT', news.topicId)} 
                                                color={reportedPosts[news.content?.contentId] ? "error" : "default"}
                                                disabled={reportedPosts[news.content?.contentId]}
                                            >
                                                <ReportIcon />
                                            </IconButton>
                                        </Tooltip>
                                    </Box>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                )}
            </Container>
        </MainLayout>
    );
}

export default MyFeed;
