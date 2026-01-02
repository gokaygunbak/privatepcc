import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import {
    AppBar, Toolbar, Typography, Button, Container, Grid, Card, CardContent, CardActions,
    Chip, Box, IconButton, Drawer, Divider, Skeleton, Tooltip, Alert
} from '@mui/material';
import {
    Favorite as FavoriteIcon,
    Bookmark as BookmarkIcon,
    FavoriteBorder, BookmarkBorder
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
        console.log(`[Interaction] ID=${id}, Type=${type}, TopicID=${topicId}`); // DEBUG
        // Optimistic UI update
        if (type === 'LIKE') {
            setLikedPosts(prev => ({ ...prev, [id]: !prev[id] }));
        } else if (type === 'SAVE') {
            setSavedPosts(prev => ({ ...prev, [id]: !prev[id] }));
        }

        try {
            const payload = {
                userId: AuthService.getCurrentUserId(),
                contentId: id,
                interactionType: type,
                topicId: topicId // Puanlama için eklendi
            };
            console.log("[Interaction] Sending payload:", payload);

            await axios.post('http://localhost:8080/api/interactions/interact', payload, {
                headers: { Authorization: `Bearer ${AuthService.getCurrentToken()}` }
            });
            console.log("[Interaction] Success!");
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
                                        <IconButton onClick={() => handleInteraction(news.summaryId, 'LIKE', news.topicId)} color={likedPosts[news.summaryId] ? "error" : "default"}>
                                            {likedPosts[news.summaryId] ? <FavoriteIcon /> : <FavoriteBorder />}
                                        </IconButton>
                                        <IconButton onClick={() => handleInteraction(news.summaryId, 'SAVE', news.topicId)} color={savedPosts[news.summaryId] ? "secondary" : "default"}>
                                            {savedPosts[news.summaryId] ? <BookmarkIcon /> : <BookmarkBorder />}
                                        </IconButton>
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
