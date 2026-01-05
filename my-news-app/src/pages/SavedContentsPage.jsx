import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import {
    Typography, Container, Grid, Card, CardContent,
    Chip, Box, IconButton, Skeleton, Alert, Button, Tooltip
} from '@mui/material';
import {
    Favorite as FavoriteIcon,
    Bookmark as BookmarkIcon,
    FavoriteBorder, BookmarkBorder,
    BookmarkRemove as BookmarkRemoveIcon,
    Report as ReportIcon,
    Link as LinkIcon
} from '@mui/icons-material';
import AuthService from '../services/AuthService';
import MainLayout from '../components/MainLayout';

function SavedContentsPage() {
    const [savedContents, setSavedContents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const navigate = useNavigate();

    // Interaction states
    const [likedPosts, setLikedPosts] = useState({});
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
        fetchSavedContents(token, userId);
    }, []);

    const fetchSavedContents = async (token, userId) => {
        try {
            const response = await axios.get(`http://localhost:8080/api/interactions/saved/${userId}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            setSavedContents(response.data);
            setLoading(false);
        } catch (error) {
            console.error("Hata:", error);
            if (error.response && error.response.status === 401) {
                AuthService.logout();
                navigate("/");
            }
            setError("Kayıtlı içerikler yüklenirken bir sorun oluştu.");
            setLoading(false);
        }
    };

    const handleInteraction = async (contentId, type, topicId) => {
        console.log(`[Interaction] ContentID=${contentId}, Type=${type}, TopicID=${topicId}`);

        if (type === 'LIKE') {
            setLikedPosts(prev => ({ ...prev, [contentId]: !prev[contentId] }));
        } else if (type === 'REPORT') {
            if (!window.confirm("Bu içeriği şikayet etmek istediğinize emin misiniz?")) {
                return;
            }
            setReportedPosts(prev => ({ ...prev, [contentId]: true }));
        }

        try {
            const payload = {
                userId: AuthService.getCurrentUserId(),
                contentId: contentId,
                interactionType: type,
                topicId: topicId
            };

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

    const handleUnsave = async (contentId, topicId) => {
        // 1. UI'dan hemen kaldır (Optimistic)
        // contentId ile eşleşen (SummaryDto içindeki content.contentId) öğeyi filtrele
        setSavedContents(prev => prev.filter(item => {
            const currentItemContentId = item.content?.contentId;
            return currentItemContentId !== contentId;
        }));

        // 2. Backend'e isteği gönder (Toggle mantığı - Unsave yapacak)
        try {
            await axios.post('http://localhost:8080/api/interactions/interact', {
                userId: AuthService.getCurrentUserId(),
                contentId: contentId,
                interactionType: 'SAVE',
                topicId: topicId
            }, {
                headers: { Authorization: `Bearer ${AuthService.getCurrentToken()}` }
            });
            console.log("[Unsave] Success for ID:", contentId);
        } catch (error) {
            console.error("Unsave failed:", error);
        }
    };

    return (
        <MainLayout title="Kaydedilenler">
            <Container maxWidth="xl">
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 4 }}>
                    <BookmarkIcon sx={{ fontSize: 40, mr: 2, color: 'secondary.main' }} />
                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                        Kaydedilen İçerikler
                    </Typography>
                </Box>

                {error && (
                    <Alert severity="error" sx={{ mb: 3 }}>
                        {error}
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
                ) : savedContents.length === 0 ? (
                    <Box sx={{
                        textAlign: 'center',
                        py: 8,
                        bgcolor: 'rgba(255,255,255,0.03)',
                        borderRadius: 4
                    }}>
                        <BookmarkBorder sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
                        <Typography variant="h5" color="text.secondary" gutterBottom>
                            Henüz kaydettiğiniz içerik yok
                        </Typography>
                        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                            Haberleri kaydetmek için yer imi simgesine tıklayın
                        </Typography>
                        <Button
                            variant="contained"
                            color="primary"
                            onClick={() => navigate("/my-feed")}
                        >
                            Haberlere Göz At
                        </Button>
                    </Box>
                ) : (
                    <Grid container spacing={3}>
                        {savedContents.map((news) => (
                            <Grid item xs={12} key={news.summaryId || news.contentId} sx= {{width: '100%'}}>
                                <Card sx={{
                                    width: '100%',
                                    display: 'flex',
                                    flexDirection: { xs: 'column', sm: 'row' },
                                    p: 2,
                                    transition: 'transform 0.2s, box-shadow 0.2s',
                                    '&:hover': {
                                        transform: 'translateY(-2px)',
                                        boxShadow: '0 8px 24px rgba(0,0,0,0.2)'
                                    }
                                }}>
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
                                        <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
                                            {news.topicName && (
                                                <Chip
                                                    label={news.topicName}
                                                    size="small"
                                                    color="secondary"
                                                    variant="outlined"
                                                />
                                            )}
                                            {news.generatedTags && news.generatedTags.split(',').map((tag, i) => (
                                                <Chip key={i} label={tag.trim()} size="small" sx={{ mr: 0.5 }} />
                                            ))}
                                        </Box>
                                    </CardContent>
                                    <Box sx={{
                                        display: 'flex',
                                        flexDirection: 'column',
                                        justifyContent: 'center',
                                        alignItems: 'center',
                                        borderLeft: { sm: '1px solid rgba(255,255,255,0.1)' },
                                        pl: { sm: 2 },
                                        pt: { xs: 2, sm: 0 }
                                    }}>
                                        <Tooltip title="Beğen">
                                            <IconButton
                                                onClick={() => handleInteraction(news.content?.contentId, 'LIKE', news.topicId)}
                                                color={likedPosts[news.content?.contentId] ? "error" : "default"}
                                            >
                                                {likedPosts[news.content?.contentId] ? <FavoriteIcon /> : <FavoriteBorder />}
                                            </IconButton>
                                        </Tooltip>
                                        <Tooltip title="Kaydedildi">
                                            <IconButton
                                                onClick={() => handleUnsave(news.content?.contentId, news.topicId)}
                                                color="secondary"
                                                sx={{
                                                    bgcolor: 'rgba(156, 39, 176, 0.1)',
                                                    '&:hover': { bgcolor: 'rgba(156, 39, 176, 0.2)' }
                                                }}
                                            >
                                                <BookmarkIcon />
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
                                        {news.sourceUrl && (
                                            <Tooltip title="Kaynağa Git">
                                                <IconButton
                                                    href={news.sourceUrl}
                                                    target="_blank"
                                                    size="small"
                                                    onClick={() => handleInteraction(news.content?.contentId, 'CLICK', news.topicId)}
                                                    sx={{ mt: 1 }}
                                                >
                                                    <LinkIcon />
                                                </IconButton>
                                            </Tooltip>
                                        )}
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

export default SavedContentsPage;

