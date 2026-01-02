import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import {
    Box,
    Button,
    Container,
    TextField,
    Typography,
    Paper,
    Avatar,
    Grid,
    Alert,
    Snackbar,
    IconButton,
    Chip,
    Divider,
    CircularProgress
} from '@mui/material';
import { 
    ArrowBack as ArrowBackIcon, 
    Save as SaveIcon, 
    AccountCircle as AccountCircleIcon,
    Category as CategoryIcon,
    Edit as EditIcon
} from '@mui/icons-material';
import AuthService from '../services/AuthService';
import MainLayout from '../components/MainLayout';

const ProfilePage = () => {
    const navigate = useNavigate();
    const [profile, setProfile] = useState({
        fullName: '',
        bio: '',
        birthDate: '',
        location: '',
        profilePictureUrl: ''
    });
    const [loading, setLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
    const [userTopics, setUserTopics] = useState([]);
    const [topicsLoading, setTopicsLoading] = useState(true);

    useEffect(() => {
        const userId = AuthService.getCurrentUserId();
        const token = AuthService.getCurrentToken();

        if (!userId || !token) {
            navigate("/");
            return;
        }

        fetchProfile(userId, token);
        fetchUserTopics(userId, token);
    }, []);

    const fetchProfile = async (userId, token) => {
        try {
            const response = await axios.get(`http://localhost:8080/api/users/profile/${userId}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            if (response.data) {
                setProfile(response.data);
            }
        } catch (error) {
            console.error("Profil yüklenirken hata:", error);
        } finally {
            setLoading(false);
        }
    };

    const fetchUserTopics = async (userId, token) => {
        console.log("Fetching topics for userId:", userId);
        try {
            const response = await axios.get(`http://localhost:8080/api/interactions/preferences/${userId}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            console.log("Topics API Response:", response.data);
            if (response.data) {
                setUserTopics(response.data);
            }
        } catch (error) {
            console.error("İlgi alanları yüklenirken hata:", error);
            console.error("Hata detayı:", error.response?.data || error.message);
        } finally {
            setTopicsLoading(false);
        }
    };

    const handleChange = (e) => {
        setProfile({ ...profile, [e.target.name]: e.target.value });
    };

    const handleImageUpload = (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onloadend = () => {
                setProfile({ ...profile, profilePictureUrl: reader.result });
            };
            reader.readAsDataURL(file);
        }
    };

    const handleSave = async (e) => {
        e.preventDefault();
        const userId = AuthService.getCurrentUserId();
        const token = AuthService.getCurrentToken();

        const payload = { ...profile };
        if (payload.birthDate === "") payload.birthDate = null;

        try {
            await axios.put(`http://localhost:8080/api/users/profile/${userId}`, payload, {
                headers: { Authorization: `Bearer ${token}` }
            });
            setSnackbar({ open: true, message: 'Profil başarıyla güncellendi!', severity: 'success' });
            setIsEditing(false); // Kayıttan sonra izleme moduna dön
        } catch (error) {
            console.error("Güncelleme hatası DETAY:", error.response ? error.response.data : error.message);
            setSnackbar({ open: true, message: 'Profil güncellenemedi.', severity: 'error' });
        }
    };

    return (
        <MainLayout title="Profilim">
            <Container maxWidth="md">
                <Button
                    startIcon={<ArrowBackIcon />}
                    onClick={() => navigate(-1)}
                    sx={{ mb: 2 }}
                >
                    Geri Dön
                </Button>

                <Paper elevation={3} sx={{ p: 4, borderRadius: 2, position: 'relative' }}>
                    {/* Düzenle Butonu (Sadece İzleme Modunda) */}
                    {!isEditing && (
                        <Button
                            variant="outlined"
                            onClick={() => setIsEditing(true)}
                            sx={{ position: 'absolute', top: 20, right: 20 }}
                        >
                            Düzenle
                        </Button>
                    )}

                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 4 }}>
                        <Avatar
                            src={profile.profilePictureUrl}
                            sx={{ width: 120, height: 120, mb: 2, bgcolor: 'primary.main', fontSize: 50, border: '4px solid white', boxShadow: 3 }}
                        >
                            {!profile.profilePictureUrl && <AccountCircleIcon sx={{ fontSize: 70 }} />}
                        </Avatar>
                        <Typography variant="h5" fontWeight="bold">
                            {profile.fullName || 'İsimsiz Kullanıcı'}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            {profile.location || 'Konum belirtilmemiş'}
                        </Typography>
                    </Box>

                    {isEditing ? (
                        <form onSubmit={handleSave}>
                            <Grid container spacing={3}>
                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        fullWidth label="Ad Soyad" name="fullName"
                                        value={profile.fullName || ''} onChange={handleChange} variant="outlined"
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        fullWidth label="Konum" name="location"
                                        value={profile.location || ''} onChange={handleChange} variant="outlined"
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        fullWidth label="Doğum Tarihi" name="birthDate" type="date"
                                        value={profile.birthDate || ''} onChange={handleChange}
                                        InputLabelProps={{ shrink: true }} variant="outlined"
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <Button
                                        variant="outlined"
                                        component="label"
                                        fullWidth
                                        sx={{ height: '100%' }}
                                    >
                                        Fotoğraf Yükle
                                        <input
                                            type="file"
                                            hidden
                                            accept="image/*"
                                            onChange={handleImageUpload}
                                        />
                                    </Button>
                                    {profile.profilePictureUrl && (
                                        <Typography variant="caption" sx={{ display: 'block', mt: 1, textAlign: 'center' }}>
                                            Resim seçildi
                                        </Typography>
                                    )}
                                </Grid>
                                <Grid item xs={12}>
                                    <TextField
                                        fullWidth label="Biyografi" name="bio"
                                        value={profile.bio || ''} onChange={handleChange}
                                        multiline rows={4} variant="outlined"
                                    />
                                </Grid>
                                <Grid item xs={12} sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                                    <Button variant="text" color="error" onClick={() => setIsEditing(false)}>
                                        İptal
                                    </Button>
                                    <Button type="submit" variant="contained" startIcon={<SaveIcon />}>
                                        Kaydet
                                    </Button>
                                </Grid>
                            </Grid>
                        </form>
                    ) : (
                        // İZLEME MODU GÖRÜNÜMÜ
                        <Grid container spacing={4} sx={{ mt: 1 }}>
                            <Grid item xs={12}>
                                <Typography variant="h6" color="primary" gutterBottom>Hakkımda</Typography>
                                <Typography variant="body1" sx={{ fontStyle: profile.bio ? 'normal' : 'italic', color: profile.bio ? 'text.primary' : 'text.disabled' }}>
                                    {profile.bio || 'Henüz bir biyografi eklenmemiş.'}
                                </Typography>
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <Box sx={{ p: 2, bgcolor: 'action.hover', borderRadius: 2 }}>
                                    <Typography variant="caption" color="text.secondary" display="block">Doğum Tarihi</Typography>
                                    <Typography variant="body1">{profile.birthDate || '-'}</Typography>
                                </Box>
                            </Grid>
                        </Grid>
                    )}

                    {/* İLGİ ALANLARI BÖLÜMÜ */}
                    <Divider sx={{ my: 4 }} />
                    <Box>
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <CategoryIcon color="primary" />
                                <Typography variant="h6" color="primary">
                                    Seçtiğim İlgi Alanlarım
                                </Typography>
                            </Box>
                            <Button 
                                size="small" 
                                startIcon={<EditIcon />}
                                onClick={() => navigate('/onboarding')}
                                variant="outlined"
                            >
                                Düzenle
                            </Button>
                        </Box>

                        {topicsLoading ? (
                            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
                                <CircularProgress size={30} />
                            </Box>
                        ) : userTopics.length > 0 ? (
                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                {userTopics.map((topic) => (
                                    <Chip
                                        key={topic.topicId}
                                        label={topic.name}
                                        color="primary"
                                        variant="outlined"
                                        sx={{ 
                                            fontSize: '0.95rem', 
                                            py: 2,
                                            borderRadius: 2,
                                            '&:hover': {
                                                bgcolor: 'primary.main',
                                                color: 'white'
                                            }
                                        }}
                                    />
                                ))}
                            </Box>
                        ) : (
                            <Box sx={{ textAlign: 'center', py: 3, bgcolor: 'action.hover', borderRadius: 2 }}>
                                <Typography color="text.secondary" gutterBottom>
                                    Henüz ilgi alanı seçmediniz.
                                </Typography>
                                <Button 
                                    variant="contained" 
                                    size="small"
                                    onClick={() => navigate('/onboarding')}
                                    sx={{ mt: 1 }}
                                >
                                    İlgi Alanlarını Seç
                                </Button>
                            </Box>
                        )}
                    </Box>
                </Paper>
            </Container>

            <Snackbar
                open={snackbar.open}
                autoHideDuration={6000}
                onClose={() => setSnackbar({ ...snackbar, open: false })}
            >
                <Alert severity={snackbar.severity} sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </MainLayout>
    );
};

export default ProfilePage;
