import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import {
  Container, Grid, Card, CardContent, CardActions,
  Chip, TextField, Box, IconButton, Divider, Tooltip, CircularProgress,
  InputAdornment, Typography, Button
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

const drawerWidth = 240;

function NewsPage() {
  const [searchResults, setSearchResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  // Interaction states
  const [likedPosts, setLikedPosts] = useState({});
  const [savedPosts, setSavedPosts] = useState({});
  const [reportedPosts, setReportedPosts] = useState({});

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchTerm.trim()) return;

    setLoading(true);
    const token = AuthService.getCurrentToken();

    try {
      // Calls the new orchestration endpoint
      // NOTE: This endpoint needs to be implemented in InteractionService
      const response = await axios.post(`http://localhost:8080/api/interactions/search?query=${encodeURIComponent(searchTerm)}`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      console.log("Search Results:", response.data);
      setSearchResults(response.data);
    } catch (error) {
      console.error("Arama hatası:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleInteraction = async (id, type, topicId) => {
    if (type === 'LIKE') {
      setLikedPosts(prev => ({ ...prev, [id]: !prev[id] }));
    } else if (type === 'SAVE') {
      setSavedPosts(prev => ({ ...prev, [id]: !prev[id] }));
    } else if (type === 'REPORT') {
      if (!window.confirm("Bu içeriği bildirmek istediğinize emin misiniz?")) return;
      setReportedPosts(prev => ({ ...prev, [id]: true }));
    }

    try {
      const token = AuthService.getCurrentToken();
      const userId = AuthService.getCurrentUserId();
      await axios.post('http://localhost:8080/api/interactions/interact', {
        userId: userId,
        contentId: id,
        interactionType: type,
        topicId: topicId
      }, {
        headers: { Authorization: `Bearer ${token}` }
      });
    } catch (e) {
      console.error("Etkileşim hatası:", e);
    }
  };

  return (
    <MainLayout title="🔍 Keşfet">
      <Container maxWidth="md" sx={{ textAlign: 'center', mb: 8, mt: 4 }}>
        <Typography variant="h3" fontWeight="bold" gutterBottom sx={{ background: 'linear-gradient(45deg, #0388a6 30%, #8bd1c3 90%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          Bugün ne okumak istiyorsun?
        </Typography>
        <Typography variant="h6" color="text.secondary" sx={{ mb: 4 }}>
          Sadece bir konu yaz, yapay zeka senin için araştırsın ve özetlesin.
        </Typography>

        <form onSubmit={handleSearch}>
          <TextField
            fullWidth
            variant="outlined"
            placeholder="Örn: Yapay Zeka, iklim krizi, vizyondaki filmler..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon color="primary" sx={{ fontSize: 30 }} />
                </InputAdornment>
              ),
              endAdornment: (
                <Button type="submit" variant="contained" size="large" sx={{ borderRadius: 28, px: 4 }}>
                  Ara
                </Button>
              ),
              sx: {
                borderRadius: 30,
                bgcolor: 'background.paper',
                py: 1.5,
                px: 2,
                fontSize: '1.2rem',
                boxShadow: 3
              }
            }}
          />
        </form>
      </Container>

      <Container maxWidth="xl">
        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
            <CircularProgress size={60} />
          </Box>
        )}

        {!loading && searchResults.length > 0 && (
          <Box>
            <Typography variant="h5" sx={{ mb: 3, fontWeight: 'bold', borderLeft: '4px solid #0388a6', pl: 2 }}>
              Sonuçlar
            </Typography>
            <Grid container spacing={3} alignItems="stretch">
              {searchResults.map((news) => (
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
                          <Chip key={i} label={tag.trim()} size="small" />
                        ))}
                      </Box>
                    </CardContent>
                    <Divider />
                    <CardActions sx={{ justifyContent: 'space-between', px: 2 }}>
                      <Box>
                        <Tooltip title="Beğen">
                          <IconButton onClick={() => handleInteraction(news.content?.contentId, 'LIKE', news.topicId)} color="primary">
                            {likedPosts[news.content?.contentId] ? <FavoriteIcon /> : <FavoriteBorder />}
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Kaydet">
                          <IconButton onClick={() => handleInteraction(news.content?.contentId, 'SAVE', news.topicId)} color="secondary">
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
                      {news.content && news.content.url && (
                        <Tooltip title="Kaynağa Git">
                          <IconButton
                            href={news.content.url}
                            target="_blank"
                            size="small"
                            onClick={() => handleInteraction(news.content?.contentId, 'CLICK', news.topicId)}
                          >
                            <LinkIcon />
                          </IconButton>
                        </Tooltip>
                      )}
                    </CardActions>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </Box>
        )}
      </Container>
    </MainLayout>
  );
}

export default NewsPage;