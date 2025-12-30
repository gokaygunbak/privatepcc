// src/pages/TrendsPage.jsx
import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import {
    AppBar, Toolbar, Typography, Button, Container, Grid, Card, CardContent, CardActions,
    Chip, TextField, Box, IconButton, Drawer, List, ListItem, ListItemText, Divider,
    Badge, InputAdornment, Skeleton, Tooltip
} from '@mui/material';
import {
    ExitToApp as LogoutIcon,
    Search as SearchIcon,
    FilterList as FilterIcon,
    Favorite as FavoriteIcon,
    Bookmark as BookmarkIcon,
    Link as LinkIcon,
    Visibility as ViewIcon,
    FavoriteBorder, BookmarkBorder,
    Home as HomeIcon,
    Person as PersonIcon,
    Check as CheckIcon,
    TrendingUp as TrendingIcon
} from '@mui/icons-material';
import AuthService from '../services/AuthService';

const drawerWidth = 240;

function TrendsPage() {
    const [summaries, setSummaries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedTags, setSelectedTags] = useState([]);
    const [searchTerm, setSearchTerm] = useState("");
    const navigate = useNavigate();

    // Interaction states
    const [likedPosts, setLikedPosts] = useState({});
    const [savedPosts, setSavedPosts] = useState({});

    useEffect(() => {
        const token = localStorage.getItem("user_token");
        if (!token) {
            navigate("/");
            return;
        }
        fetchSummaries(token);
    }, []);

    const fetchSummaries = async (token) => {
        try {
            const response = await axios.get('http://localhost:8080/api/llm/summaries', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setSummaries(response.data);
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

    const handleLogout = () => {
        localStorage.removeItem("user_token");
        navigate("/");
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

    const handleInteraction = async (id, type, topicId) => {
        if (type === 'LIKE') {
            setLikedPosts(prev => ({ ...prev, [id]: !prev[id] }));
        } else if (type === 'SAVE') {
            setSavedPosts(prev => ({ ...prev, [id]: !prev[id] }));
        }

        try {
            const token = localStorage.getItem("user_token");
            await axios.post('http://localhost:8080/api/interactions/interact', {
                userId: 1, // TODO: Replace with dynamic user ID
                contentId: id,
                interactionType: type,
                topicId: topicId
            }, {
                headers: { Authorization: `Bearer ${token}` }
            });
            console.log(`[TrendsPage] Interaction sent: ${type} for topic ${topicId}`);
        } catch (e) {
            console.error("Etkileşim hatası:", e);
        }
    };

    return (
        <MainLayout title="Bu Günlerde (Tüm Haberler)">
            {/* Filter Toolbar embedded in content or just leave common layout */}
            <Box sx={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Box sx={{ display: 'flex', gap: 2, mb: 3, width: '100%', maxWidth: 'xl', justifyContent: 'flex-end' }}>
                    <TextField
                        variant="outlined"
                        size="small"
                        placeholder="Filtrele..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchIcon color="action" />
                                </InputAdornment>
                            ),
                            sx: { borderRadius: 4, bgcolor: 'background.paper', width: { xs: 120, sm: 200 } }
                        }}
                    />
                </Box>

                <Box sx={{ width: '100%', maxWidth: 'xl', mb: 3, p: 2, bgcolor: 'background.paper', borderRadius: 2 }}>
                    <Typography variant="h6" sx={{ mb: 2, fontWeight: 'bold', color: 'primary.main' }}>
                        Konulara Göre Filtrele
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
                            variant="outlined"
                            color="error"
                            size="small"
                            sx={{ mt: 2 }}
                            onClick={() => setSelectedTags([])}
                        >
                            Filtreleri Temizle
                        </Button>
                    )}
                </Box>
            </Box>

            <Container maxWidth="xl">
                <Typography variant="h4" sx={{ mb: 4, fontWeight: 'bold' }}>
                    En Son Gelişmeler
                </Typography>

                {loading ? (
                    <Grid container spacing={3}>
                        {[1, 2, 3, 4].map((item) => (
                            <Grid item xs={12} sm={6} md={4} lg={3} key={item}>
                                <Skeleton variant="rectangular" height={200} sx={{ borderRadius: 2 }} />
                                <Skeleton width="60%" sx={{ mt: 1 }} />
                                <Skeleton width="80%" />
                            </Grid>
                        ))}
                    </Grid>
                ) : (
                    <Grid container spacing={3} alignItems="stretch">
                        {filteredSummaries.map((news) => (
                            <Grid item xs={12} sm={6} md={4} lg={3} key={news.summaryId}>
                                <Card sx={{
                                    width: '100%',
                                    display: 'flex',
                                    flexDirection: 'column',
                                    justifyContent: 'space-between',
                                    transition: 'transform 0.2s',
                                    '&:hover': { transform: 'translateY(-4px)', boxShadow: 6 }
                                }}>
                                    <CardContent sx={{ flexGrow: 1 }}>
                                        <Typography gutterBottom variant="h6" component="div" color="primary" sx={{
                                            display: '-webkit-box',
                                            overflow: 'hidden',
                                            WebkitBoxOrient: 'vertical',
                                            WebkitLineClamp: 2,
                                        }}>
                                            {news.title}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{
                                            mb: 2,
                                            display: '-webkit-box',
                                            overflow: 'hidden',
                                            WebkitBoxOrient: 'vertical',
                                            WebkitLineClamp: 4,
                                        }}>
                                            {news.summaryText}
                                        </Typography>
                                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                            {news.generatedTags && news.generatedTags.split(',').map((tag, i) => (
                                                <Chip key={i} label={tag.trim()} size="small" onClick={() => toggleTag(tag.trim())} sx={{ cursor: 'pointer' }} />
                                            ))}
                                        </Box>
                                    </CardContent>
                                    <Divider />
                                    <CardActions sx={{ justifyContent: 'space-between', px: 2 }}>
                                        <Box>
                                            <Tooltip title="Beğen">
                                                <IconButton onClick={() => handleInteraction(news.summaryId, 'LIKE', news.topicId)} color="primary">
                                                    {likedPosts[news.summaryId] ? <FavoriteIcon /> : <FavoriteBorder />}
                                                </IconButton>
                                            </Tooltip>
                                            <Tooltip title="Kaydet">
                                                <IconButton onClick={() => handleInteraction(news.summaryId, 'SAVE', news.topicId)} color="secondary">
                                                    {savedPosts[news.summaryId] ? <BookmarkIcon /> : <BookmarkBorder />}
                                                </IconButton>
                                            </Tooltip>
                                        </Box>
                                        {news.content && news.content.url && (
                                            <Tooltip title="Kaynağa Git">
                                                <IconButton href={news.content.url} target="_blank" size="small">
                                                    <LinkIcon />
                                                </IconButton>
                                            </Tooltip>
                                        )}
                                    </CardActions>
                                </Card>
                            </Grid>
                        ))}
                    </Grid>
                )}
            </Container>
        </MainLayout>
    );
}

export default TrendsPage;
