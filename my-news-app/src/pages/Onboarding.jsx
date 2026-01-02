import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import {
    Box,
    Typography,
    Container,
    Paper,
    Chip,
    Button,
    Grid,
    Alert,
    CircularProgress,
    TextField,
    InputAdornment
} from '@mui/material';
import {
    Check as CheckIcon,
    Search as SearchIcon,
    ArrowForward as ArrowForwardIcon
} from '@mui/icons-material';
import AuthService from '../services/AuthService';

function Onboarding() {
    const [topics, setTopics] = useState([]);
    const [selectedTopicIds, setSelectedTopicIds] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [searchTerm, setSearchTerm] = useState("");
    const navigate = useNavigate();

    useEffect(() => {
        const token = AuthService.getCurrentToken();
        if (!token) {
            navigate("/");
            return;
        }
        fetchTopicsAndPreferences();
    }, []);

    const fetchTopicsAndPreferences = async () => {
        const token = AuthService.getCurrentToken();
        const userId = AuthService.getCurrentUserId();
        
        try {
            // 1. Tüm konuları çek
            const topicsResponse = await axios.get('http://localhost:8080/api/interactions/topics', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setTopics(topicsResponse.data);

            // 2. Kullanıcının mevcut tercihlerini çek
            if (userId) {
                try {
                    const prefsResponse = await axios.get(`http://localhost:8080/api/interactions/preferences/${userId}`, {
                        headers: { Authorization: `Bearer ${token}` }
                    });
                    // Mevcut tercihleri seçili olarak işaretle
                    if (prefsResponse.data && prefsResponse.data.length > 0) {
                        const existingTopicIds = prefsResponse.data.map(topic => topic.topicId);
                        setSelectedTopicIds(existingTopicIds);
                        console.log("Mevcut tercihler yüklendi:", existingTopicIds);
                    }
                } catch (prefErr) {
                    console.log("Mevcut tercih bulunamadı (ilk kez seçim yapılacak)");
                }
            }

            setLoading(false);
        } catch (err) {
            console.error("Konular çekilemedi:", err);
            setError("Konular yüklenirken bir hata oluştu. Lütfen daha sonra tekrar deneyin.");
            setLoading(false);
        }
    };

    const toggleTopic = (id) => {
        if (selectedTopicIds.includes(id)) {
            setSelectedTopicIds(selectedTopicIds.filter(topicId => topicId !== id));
        } else {
            setSelectedTopicIds([...selectedTopicIds, id]);
        }
    };

    const handleSave = async () => {
        const userId = AuthService.getCurrentUserId();
        if (!userId) {
            setError(
                <Box>
                    Kullanıcı kimliği bulunamadı. Lütfen tekrar giriş yapın.
                    <Button color="inherit" size="small" onClick={() => navigate("/")} sx={{ ml: 1, textDecoration: 'underline' }}>
                        Giriş Yap
                    </Button>
                </Box>
            );
            return;
        }

        // Hiç seçim yoksa kullanıcıya onay sor
        if (selectedTopicIds.length === 0) {
            const confirmed = window.confirm(
                "Hiç ilgi alanı seçmediniz. Tüm tercihleriniz ve skorlarınız sıfırlanacak. Devam etmek istiyor musunuz?"
            );
            if (!confirmed) return;
        }

        setSaving(true);
        try {
            await axios.post('http://localhost:8080/api/interactions/preferences', {
                userId: userId,
                topicIds: selectedTopicIds
            }, {
                headers: { Authorization: `Bearer ${AuthService.getCurrentToken()}` }
            });

            // Başarılı olursa kişisel akışa yönlendir
            navigate("/my-feed");
        } catch (err) {
            console.error("Tercihler kaydedilemedi:", err);
            setError("Tercihler kaydedilirken bir sorun oluştu.");
            setSaving(false);
        }
    };

    const filteredTopics = topics.filter(t =>
        t.name.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <Box sx={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            bgcolor: 'background.default',
            p: 3
        }}>
            <Container maxWidth="md">
                <Paper
                    elevation={10}
                    sx={{
                        p: 5,
                        borderRadius: 4,
                        backdropFilter: 'blur(10px)',
                        backgroundColor: 'rgba(30, 30, 30, 0.6)',
                        border: '1px solid rgba(255,255,255,0.1)'
                    }}
                >
                    <Box sx={{ textAlign: 'center', mb: 4 }}>
                        <Typography variant="h3" fontWeight="bold" gutterBottom sx={{
                            background: 'linear-gradient(45deg, #CE93D8 30%, #4DB6AC 90%)',
                            WebkitBackgroundClip: 'text',
                            WebkitTextFillColor: 'transparent'
                        }}>
                            İlgi Alanlarını Seç
                        </Typography>
                        <Typography variant="h6" color="text.secondary">
                            {selectedTopicIds.length > 0 
                                ? `${selectedTopicIds.length} alan seçili. Ekle veya çıkar.`
                                : "Sana en uygun haberleri getirebilmemiz için neleri sevdiğini söyle."
                            }
                        </Typography>
                    </Box>

                    {error && (
                        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError("")}>
                            {error}
                        </Alert>
                    )}

                    {loading ? (
                        <Box sx={{ display: 'flex', justifyContent: 'center', my: 5 }}>
                            <CircularProgress color="secondary" />
                        </Box>
                    ) : (
                        <>
                            <Box sx={{ mb: 4 }}>
                                <TextField
                                    fullWidth
                                    variant="outlined"
                                    placeholder="Konularda ara..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                    InputProps={{
                                        startAdornment: (
                                            <InputAdornment position="start">
                                                <SearchIcon color="action" />
                                            </InputAdornment>
                                        ),
                                        sx: { borderRadius: 3, bgcolor: 'background.paper' }
                                    }}
                                />
                            </Box>

                            <Box sx={{
                                display: 'flex',
                                flexWrap: 'wrap',
                                gap: 1.5,
                                justifyContent: 'center',
                                minHeight: 200,
                                maxHeight: 400,
                                overflowY: 'auto',
                                p: 1
                            }}>
                                {filteredTopics.map((topic, index) => {
                                    const isSelected = selectedTopicIds.includes(topic.topicId);
                                    return (
                                        <Chip
                                            key={`${topic.topicId}-${index}`}
                                            label={topic.name}
                                            onClick={() => toggleTopic(topic.topicId)}
                                            icon={isSelected ? <CheckIcon /> : undefined}
                                            color={isSelected ? "secondary" : "default"}
                                            variant={isSelected ? "filled" : "outlined"}
                                            clickable
                                            sx={{
                                                fontSize: '1rem',
                                                p: 1.5,
                                                borderRadius: '16px',
                                                transition: 'all 0.2s',
                                                transform: isSelected ? 'scale(1.05)' : 'scale(1)',
                                                border: isSelected ? 'none' : '1px solid rgba(255,255,255,0.2)'
                                            }}
                                        />
                                    );
                                })}
                                {filteredTopics.length === 0 && (
                                    <Typography color="text.secondary">Aradığınız konu bulunamadı.</Typography>
                                )}
                            </Box>

                            <Box sx={{ mt: 5, display: 'flex', justifyContent: 'space-between' }}>
                                <Button
                                    variant="outlined"
                                    color="inherit"
                                    size="large"
                                    onClick={() => navigate(-1)}
                                    sx={{
                                        borderRadius: 3,
                                        px: 3,
                                        py: 1.5,
                                    }}
                                >
                                    Geri
                                </Button>

                                <Button
                                    variant="contained"
                                    color="primary"
                                    size="large"
                                    endIcon={saving ? <CircularProgress size={20} color="inherit" /> : <ArrowForwardIcon />}
                                    onClick={handleSave}
                                    disabled={saving}
                                    sx={{
                                        borderRadius: 3,
                                        px: 4,
                                        py: 1.5,
                                        fontWeight: 'bold',
                                        background: selectedTopicIds.length === 0 
                                            ? 'linear-gradient(45deg, #757575 30%, #616161 90%)'
                                            : 'linear-gradient(45deg, #673AB7 30%, #512DA8 90%)',
                                    }}
                                >
                                    {saving ? "Kaydediliyor..." : selectedTopicIds.length === 0 ? "Tümünü Kaldır" : "Devam Et"}
                                </Button>
                            </Box>
                        </>
                    )}
                </Paper>
            </Container>
        </Box>
    );
}

export default Onboarding;
