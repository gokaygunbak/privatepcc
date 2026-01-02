import React, { useState, useEffect } from 'react';
import {
    Box,
    Typography,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Card,
    CardContent,
    Grid,
    Chip,
    CircularProgress,
    Alert,
    Divider,
    Button,
    IconButton,
    Tooltip
} from '@mui/material';
import {
    People as PeopleIcon,
    Article as ArticleIcon,
    Category as CategoryIcon,
    TrendingUp as TrendingUpIcon,
    AdminPanelSettings as AdminIcon,
    ArrowBack as ArrowBackIcon,
    Report as ReportIcon,
    Delete as DeleteIcon,
    Warning as WarningIcon
} from '@mui/icons-material';
import axios from 'axios';
import AuthService from '../services/AuthService';
import { useNavigate } from 'react-router-dom';

const AdminPage = () => {
    const navigate = useNavigate();
    const [stats, setStats] = useState({
        totalUsers: 0,
        totalContents: 0,
        totalTopics: 0,
        totalInteractions: 0
    });
    const [topics, setTopics] = useState([]);
    const [reportedContents, setReportedContents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [deleting, setDeleting] = useState({});

    useEffect(() => {
        // Admin kontrolü
        if (!AuthService.isAdmin()) {
            navigate('/trends');
            return;
        }

        const token = AuthService.getCurrentToken();
        if (!token) {
            navigate('/login');
            return;
        }

        fetchAdminData(token);
    }, [navigate]);

    const fetchAdminData = async (token) => {
        try {
            const headers = { Authorization: `Bearer ${token}` };

            // Topic'leri çek
            const topicsRes = await axios.get('http://localhost:8080/api/llm/topics', { headers });
            setTopics(topicsRes.data);

            // Şikayet edilen içerikleri çek
            try {
                const reportsRes = await axios.get('http://localhost:8080/api/interactions/reports', { headers });
                setReportedContents(reportsRes.data);
            } catch (e) {
                console.log('Şikayet edilen içerik yok veya hata:', e);
                setReportedContents([]);
            }

            // İstatistikleri paralel olarak çek
            let userCount = '-';
            let contentCount = '-';
            let interactionCount = '-';

            try {
                const userCountRes = await axios.get('http://localhost:8080/api/auth/stats/user-count', { headers });
                userCount = userCountRes.data;
            } catch (e) {
                console.log('Kullanıcı sayısı alınamadı:', e);
            }

            try {
                const summaryCountRes = await axios.get('http://localhost:8080/api/llm/stats/summary-count', { headers });
                contentCount = summaryCountRes.data;
            } catch (e) {
                console.log('Özet sayısı alınamadı:', e);
            }

            try {
                const interactionCountRes = await axios.get('http://localhost:8080/api/interactions/stats/interaction-count', { headers });
                interactionCount = interactionCountRes.data;
            } catch (e) {
                console.log('Etkileşim sayısı alınamadı:', e);
            }

            // İstatistikleri set et
            setStats({
                totalUsers: userCount,
                totalContents: contentCount,
                totalTopics: topicsRes.data.length,
                totalInteractions: interactionCount
            });

            setLoading(false);
        } catch (err) {
            console.error('Admin veri çekme hatası:', err);
            setError('Veriler yüklenirken hata oluştu.');
            setLoading(false);
        }
    };

    const handleDeleteContent = async (contentId) => {
        if (!window.confirm("Bu içeriği kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz!")) {
            return;
        }

        setDeleting(prev => ({ ...prev, [contentId]: true }));

        try {
            const token = AuthService.getCurrentToken();
            await axios.delete(`http://localhost:8080/api/interactions/content/${contentId}`, {
                headers: { Authorization: `Bearer ${token}` }
            });

            // UI'dan kaldır
            setReportedContents(prev => prev.filter(item => item.content?.contentId !== contentId));
            alert("İçerik başarıyla silindi!");
        } catch (err) {
            console.error('Silme hatası:', err);
            alert("İçerik silinirken hata oluştu: " + (err.response?.data || err.message));
        } finally {
            setDeleting(prev => ({ ...prev, [contentId]: false }));
        }
    };

    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
                <CircularProgress size={60} />
            </Box>
        );
    }

    if (error) {
        return (
            <Box sx={{ p: 3 }}>
                <Alert severity="error">{error}</Alert>
            </Box>
        );
    }

    return (
        <Box sx={{ p: 4, maxWidth: 1200, mx: 'auto' }}>
            {/* Header */}
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 4 }}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <AdminIcon sx={{ fontSize: 40, color: 'error.main', mr: 2 }} />
                    <Typography variant="h4" sx={{ fontWeight: 'bold', color: 'text.primary' }}>
                        Admin Paneli
                    </Typography>
                </Box>
                <Button
                    variant="outlined"
                    startIcon={<ArrowBackIcon />}
                    onClick={() => navigate('/trends')}
                    sx={{ borderRadius: 2 }}
                >
                    Ana Sayfaya Dön
                </Button>
            </Box>

            {/* İstatistik Kartları */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
                <Grid item xs={12} sm={6} md={3}>
                    <Card sx={{ 
                        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                        color: 'white'
                    }}>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                <Box>
                                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                                        {stats.totalUsers}
                                    </Typography>
                                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                                        Toplam Kullanıcı
                                    </Typography>
                                </Box>
                                <PeopleIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                    <Card sx={{ 
                        background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
                        color: 'white'
                    }}>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                <Box>
                                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                                        {stats.totalContents}
                                    </Typography>
                                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                                        Toplam İçerik
                                    </Typography>
                                </Box>
                                <ArticleIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                    <Card sx={{ 
                        background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
                        color: 'white'
                    }}>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                <Box>
                                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                                        {stats.totalTopics}
                                    </Typography>
                                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                                        Toplam Kategori
                                    </Typography>
                                </Box>
                                <CategoryIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                    <Card sx={{ 
                        background: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
                        color: 'white'
                    }}>
                        <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                <Box>
                                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                                        {stats.totalInteractions}
                                    </Typography>
                                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                                        Toplam Etkileşim
                                    </Typography>
                                </Box>
                                <TrendingUpIcon sx={{ fontSize: 48, opacity: 0.8 }} />
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>

            <Divider sx={{ my: 4 }} />

            {/* Şikayet Edilen İçerikler */}
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <ReportIcon sx={{ fontSize: 30, color: 'error.main', mr: 1 }} />
                <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
                    Şikayet Edilen İçerikler
                </Typography>
                {reportedContents.length > 0 && (
                    <Chip 
                        label={reportedContents.length} 
                        color="error" 
                        size="small" 
                        sx={{ ml: 2 }}
                    />
                )}
            </Box>

            {reportedContents.length === 0 ? (
                <Alert severity="success" sx={{ mb: 4 }}>
                    🎉 Şikayet edilen içerik bulunmuyor!
                </Alert>
            ) : (
                <TableContainer component={Paper} sx={{ boxShadow: 3, mb: 4 }}>
                    <Table>
                        <TableHead sx={{ bgcolor: 'error.main' }}>
                            <TableRow>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Başlık</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Özet</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Kategori</TableCell>
                                <TableCell sx={{ color: 'white', fontWeight: 'bold', textAlign: 'center' }}>İşlem</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {reportedContents.map((content) => (
                                <TableRow key={content.summaryId || content.content?.contentId} hover>
                                    <TableCell sx={{ maxWidth: 200 }}>
                                        <Typography variant="body2" sx={{ 
                                            fontWeight: 'bold',
                                            display: '-webkit-box',
                                            overflow: 'hidden',
                                            WebkitBoxOrient: 'vertical',
                                            WebkitLineClamp: 2
                                        }}>
                                            {content.title}
                                        </Typography>
                                    </TableCell>
                                    <TableCell sx={{ maxWidth: 300 }}>
                                        <Typography variant="body2" color="text.secondary" sx={{
                                            display: '-webkit-box',
                                            overflow: 'hidden',
                                            WebkitBoxOrient: 'vertical',
                                            WebkitLineClamp: 2
                                        }}>
                                            {content.summaryText}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        {content.topicName && (
                                            <Chip label={content.topicName} size="small" color="secondary" />
                                        )}
                                    </TableCell>
                                    <TableCell sx={{ textAlign: 'center' }}>
                                        <Tooltip title="İçeriği Sil">
                                            <IconButton 
                                                color="error"
                                                onClick={() => handleDeleteContent(content.content?.contentId)}
                                                disabled={deleting[content.content?.contentId]}
                                            >
                                                {deleting[content.content?.contentId] ? (
                                                    <CircularProgress size={24} color="error" />
                                                ) : (
                                                    <DeleteIcon />
                                                )}
                                            </IconButton>
                                        </Tooltip>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}

            <Divider sx={{ my: 4 }} />

            {/* Kategoriler Tablosu */}
            <Typography variant="h5" sx={{ mb: 3, fontWeight: 'bold' }}>
                📚 Kategoriler
            </Typography>
            <TableContainer component={Paper} sx={{ boxShadow: 3 }}>
                <Table>
                    <TableHead sx={{ bgcolor: 'primary.main' }}>
                        <TableRow>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>ID</TableCell>
                            <TableCell sx={{ color: 'white', fontWeight: 'bold' }}>Kategori Adı</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {topics.map((topic) => (
                            <TableRow key={topic.topicId} hover>
                                <TableCell>
                                    <Chip label={topic.topicId} size="small" color="primary" />
                                </TableCell>
                                <TableCell>{topic.name}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Gelecekte eklenebilecek özellikler */}
            <Box sx={{ mt: 4, p: 3, bgcolor: 'grey.100', borderRadius: 2 }}>
                <Typography variant="h6" sx={{ mb: 2, color: 'text.secondary' }}>
                    🚧 Geliştirme Aşamasında
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    • Kullanıcı yönetimi (ban, rol değiştirme)<br />
                    • Detaylı istatistikler ve grafikler<br />
                    • RSS kaynak yönetimi
                </Typography>
            </Box>
        </Box>
    );
};

export default AdminPage;

