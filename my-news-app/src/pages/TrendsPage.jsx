// src/pages/TrendsPage.jsx
import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import {
    Typography, Button, Container, Grid, Card, CardContent, CardActions,
    Chip, TextField, Box, IconButton, Divider,
    InputAdornment, Skeleton, Tooltip, Pagination
} from '@mui/material';
import {
    Search as SearchIcon,
    Favorite as FavoriteIcon,
    Bookmark as BookmarkIcon,
    Link as LinkIcon,
    FavoriteBorder, BookmarkBorder,
    Report as ReportIcon
} from '@mui/icons-material';
import AuthService from '../services/AuthService';

function TrendsPage() {
    const [summaries, setSummaries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedTags, setSelectedTags] = useState([]);
    const [searchTerm, setSearchTerm] = useState("");
    const navigate = useNavigate();

    // Pagination states
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const pageSize = 12; // Grid düzeni için 4'ün katı olması daha iyi

    // Cache State for Random Session Consistency
    // keeps track of { pageIndex: [summaries] }
    const [pagesCache, setPagesCache] = useState({});

    // Interaction states
    const [likedPosts, setLikedPosts] = useState({});
    const [savedPosts, setSavedPosts] = useState({});
    const [reportedPosts, setReportedPosts] = useState({});

    useEffect(() => {
        const token = localStorage.getItem("user_token");
        if (!token) {
            navigate("/");
            return;
        }

        // Eğer bu sayfa cache'de varsa oradan çek, yoksa backend'e git
        if (pagesCache[currentPage]) {
            setSummaries(pagesCache[currentPage]);
            setLoading(false);
        } else {
            fetchSummaries(token, currentPage);
        }
    }, [currentPage]); // pagesCache dependency eklenmemeli, loop olabilir

    const fetchSummaries = async (token, page) => {
        setLoading(true);
        try {
            // Önceki sayfalardaki tüm ID'leri topla (Exclusion List)
            const excludeIds = Object.values(pagesCache).flatMap(list => list.map(s => s.content?.contentId)).filter(id => id);

            const userId = AuthService.getCurrentUserId();

            const response = await axios.post('http://localhost:8080/api/llm/feed/trends', {
                userId: userId,
                excludeIds: excludeIds,
                size: pageSize
            }, {
                headers: { Authorization: `Bearer ${token}` }
            });

            const newSummaries = response.data.content;

            // State güncelle
            setSummaries(newSummaries);
            setTotalPages(response.data.totalPages);
            setTotalElements(response.data.totalElements);

            // Cache'e yaz
            setPagesCache(prev => ({ ...prev, [page]: newSummaries }));

            setLoading(false);
        } catch (error) {
            console.error("Hata:", error);
            if (error.response && error.response.status === 401) {
                localStorage.removeItem("user_token");
                navigate("/");
            }
            setLoading(false);
        }
    };

    const handlePageChange = (event, value) => {
        setCurrentPage(value - 1); // MUI Pagination 1-indexed, backend 0-indexed
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const toggleTag = (tag) => {
        if (selectedTags.includes(tag)) {
            setSelectedTags(selectedTags.filter(t => t !== tag));
        } else {
            setSelectedTags([...selectedTags, tag]);
        }
    };

    const allTags = [...new Set(summaries.flatMap(s => s.generatedTags ? s.generatedTags.split(',').map(t => t.trim()) : []))];

    const filteredSummaries = summaries.filter(news => {
        const newsTags = news.generatedTags ? news.generatedTags.split(',').map(t => t.trim()) : [];
        const matchesTags = selectedTags.length === 0 || selectedTags.every(tag => newsTags.includes(tag));
        const searchLower = searchTerm.toLowerCase();
        const matchesSearch = news.title.toLowerCase().includes(searchLower) ||
            news.summaryText.toLowerCase().includes(searchLower);
        return matchesTags && matchesSearch;
    });

    const handleInteraction = async (contentId, type, topicId) => {
        if (type === 'LIKE') {
            setLikedPosts(prev => ({ ...prev, [contentId]: !prev[contentId] }));
        } else if (type === 'SAVE') {
            setSavedPosts(prev => ({ ...prev, [contentId]: !prev[contentId] }));
        } else if (type === 'REPORT') {
            // Şikayet için onay al
            if (!window.confirm("Bu içeriği şikayet etmek istediğinize emin misiniz?")) {
                return;
            }
            setReportedPosts(prev => ({ ...prev, [contentId]: true }));
        }

        try {
            const token = AuthService.getCurrentToken();
            const userId = AuthService.getCurrentUserId();

            await axios.post('http://localhost:8080/api/interactions/interact', {
                userId: userId,
                contentId: contentId,
                interactionType: type,
                topicId: topicId
            }, {
                headers: { Authorization: `Bearer ${token}` }
            });
            console.log(`[TrendsPage] Interaction sent: ${type} for contentId=${contentId}, topicId=${topicId}`);

            if (type === 'REPORT') {
                alert("Şikayetiniz admin'e iletildi. Teşekkürler!");
            }
        } catch (e) {
            console.error("Etkileşim hatası:", e);
        }
    };

    return (
        <MainLayout title="Genel Bakış">
            <Box sx={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                {/* Header with info and search */}
                <Box sx={{ display: 'flex', gap: 2, mb: 3, width: '100%', maxWidth: 'xl', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap' }}>
                    <Typography variant="body2" color="text.secondary">
                        Toplam {totalElements} haber • Sayfa {currentPage + 1} / {totalPages || 1}
                    </Typography>
                    <TextField
                        variant="outlined"
                        size="small"
                        placeholder="Bu sayfada filtrele..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchIcon color="action" />
                                </InputAdornment>
                            ),
                            sx: { borderRadius: 4, bgcolor: 'background.paper', width: { xs: 180, sm: 250 } }
                        }}
                    />
                </Box>

                {/* Tag Filter */}
                {allTags.length > 0 && (
                    <Box sx={{ width: '100%', maxWidth: 'xl', mb: 3, p: 2, bgcolor: 'background.paper', borderRadius: 2 }}>
                        <Typography variant="subtitle2" sx={{ mb: 1.5, fontWeight: 'bold', color: 'text.secondary' }}>
                            Bu sayfadaki konular:
                        </Typography>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                            {allTags.map((tag) => (
                                <Chip
                                    key={tag}
                                    label={tag}
                                    onClick={() => toggleTag(tag)}
                                    color={selectedTags.includes(tag) ? "primary" : "default"}
                                    variant={selectedTags.includes(tag) ? "filled" : "outlined"}
                                    clickable
                                    size="small"
                                    sx={{ transition: 'all 0.2s' }}
                                />
                            ))}
                        </Box>
                        {selectedTags.length > 0 && (
                            <Button
                                variant="text"
                                color="error"
                                size="small"
                                sx={{ mt: 1.5 }}
                                onClick={() => setSelectedTags([])}
                            >
                                Filtreleri Temizle
                            </Button>
                        )}
                    </Box>
                )}
            </Box>

            <Container maxWidth="xl">
                <Typography variant="h4" sx={{ mb: 4, fontWeight: 'bold' }}>
                    Geçtiğimiz Günlerde
                </Typography>

                {loading ? (
                    <Grid container spacing={3}>
                        {[1, 2, 3, 4, 5, 6, 7, 8].map((item) => (
                            <Grid item xs={12} key={item}>
                                <Skeleton variant="rectangular" height={200} sx={{ borderRadius: 2 }} />
                                <Skeleton width="60%" sx={{ mt: 1 }} />
                                <Skeleton width="80%" />
                            </Grid>
                        ))}
                    </Grid>
                ) : (
                    <>
                        <Grid container spacing={3} alignItems="stretch">
                            {filteredSummaries.map((news) => (
                                <Grid item xs={12} sm={6} md={4} lg={3} key={news.summaryId} sx={{ display: 'flex' }}>
                                    <Card sx={{
                                        width: '100%',
                                        height: '100%',
                                        display: 'flex',
                                        flexDirection: 'column',
                                        justifyContent: 'space-between',
                                        transition: 'transform 0.2s, box-shadow 0.2s',
                                        '&:hover': { transform: 'translateY(-4px)', boxShadow: 6 }
                                    }}>
                                        <CardContent sx={{ flexGrow: 1 }}>
                                            {news.topicName && (
                                                <Chip
                                                    label={news.topicName}
                                                    size="small"
                                                    color="secondary"
                                                    sx={{ mb: 1, fontSize: '0.7rem' }}
                                                />
                                            )}
                                            <Typography gutterBottom variant="h6" component="div" color="primary" sx={{
                                                display: '-webkit-box',
                                                overflow: 'hidden',
                                                WebkitBoxOrient: 'vertical',
                                                WebkitLineClamp: 2,
                                                lineHeight: 1.3
                                            }}>
                                                {news.title}
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary" sx={{
                                                mb: 2,
                                                display: '-webkit-box',
                                                overflow: 'hidden',
                                                WebkitBoxOrient: 'vertical',
                                                WebkitLineClamp: 3,
                                            }}>
                                                {news.summaryText}
                                            </Typography>
                                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                                {news.generatedTags && news.generatedTags.split(',').slice(0, 3).map((tag, i) => (
                                                    <Chip
                                                        key={i}
                                                        label={tag.trim()}
                                                        size="small"
                                                        variant="outlined"
                                                        onClick={() => toggleTag(tag.trim())}
                                                        sx={{ cursor: 'pointer', fontSize: '0.7rem' }}
                                                    />
                                                ))}
                                            </Box>
                                        </CardContent>
                                        <Divider />
                                        <CardActions sx={{ justifyContent: 'space-between', px: 2 }}>
                                            <Box>
                                                <Tooltip title="Beğen">
                                                    <IconButton onClick={() => handleInteraction(news.content?.contentId, 'LIKE', news.topicId)} color="primary" size="small">
                                                        {likedPosts[news.content?.contentId] ? <FavoriteIcon /> : <FavoriteBorder />}
                                                    </IconButton>
                                                </Tooltip>
                                                <Tooltip title="Kaydet">
                                                    <IconButton onClick={() => handleInteraction(news.content?.contentId, 'SAVE', news.topicId)} color="secondary" size="small">
                                                        {savedPosts[news.content?.contentId] ? <BookmarkIcon /> : <BookmarkBorder />}
                                                    </IconButton>
                                                </Tooltip>
                                                <Tooltip title="Şikayet Et">
                                                    <IconButton
                                                        onClick={() => handleInteraction(news.content?.contentId, 'REPORT', news.topicId)}
                                                        color={reportedPosts[news.content?.contentId] ? "error" : "default"}
                                                        size="small"
                                                        disabled={reportedPosts[news.content?.contentId]}
                                                    >
                                                        <ReportIcon fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                            </Box>
                                            {news.sourceUrl && (
                                                <Tooltip title="Kaynağa Git">
                                                    <IconButton
                                                        href={news.sourceUrl}
                                                        target="_blank"
                                                        size="small"
                                                        onClick={() => handleInteraction(news.content?.contentId, 'CLICK', news.topicId)}
                                                    >
                                                        <LinkIcon fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                            )}
                                        </CardActions>
                                    </Card>
                                </Grid>
                            ))}
                        </Grid>

                        {/* Pagination */}
                        {totalPages > 1 && (
                            <Box sx={{
                                display: 'flex',
                                justifyContent: 'center',
                                mt: 5,
                                mb: 3,
                                p: 2,
                                bgcolor: 'background.paper',
                                borderRadius: 2
                            }}>
                                <Pagination
                                    count={totalPages}
                                    page={currentPage + 1}
                                    onChange={handlePageChange}
                                    color="primary"
                                    size="large"
                                    showFirstButton
                                    showLastButton
                                    sx={{
                                        '& .MuiPaginationItem-root': {
                                            fontSize: '1rem',
                                        }
                                    }}
                                />
                            </Box>
                        )}

                        {filteredSummaries.length === 0 && !loading && (
                            <Box sx={{ textAlign: 'center', py: 5 }}>
                                <Typography variant="h6" color="text.secondary">
                                    {searchTerm || selectedTags.length > 0
                                        ? "Filtrelere uygun içerik bulunamadı"
                                        : "Henüz içerik yok"}
                                </Typography>
                            </Box>
                        )}
                    </>
                )}
            </Container>
        </MainLayout>
    );
}

export default TrendsPage;
