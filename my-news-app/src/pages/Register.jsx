// src/pages/Register.jsx
import React, { useState } from 'react';
import AuthService from '../services/AuthService';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { Box, Button, TextField, Typography, Container, Paper, Link, Alert, CircularProgress } from '@mui/material';

const Register = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState(false);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleRegister = async (e) => {
        e.preventDefault();
        setError("");
        setSuccess(false);
        setLoading(true);

        try {
            await AuthService.register(username, password);
            setSuccess(true);
            setTimeout(() => {
                navigate("/");
            }, 2000);
        } catch (err) {
            setError("Kayıt başarısız! Kullanıcı adı alınmış olabilir.");
            setLoading(false);
        }
    };

    return (
        <Box
            sx={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'linear-gradient(135deg, #f4f9f9 0%, #e0f2f1 100%)', // Soft Mint/White Gradient
            }}
        >
            <Container maxWidth="xs">
                <Paper
                    elevation={6}
                    sx={{
                        p: 4,
                        borderRadius: 3,
                        backdropFilter: 'blur(10px)',
                        backgroundColor: 'rgba(255, 255, 255, 0.9)', // White Glassmorphism
                        border: '1px solid rgba(255, 255, 255, 0.3)',
                        textAlign: 'center',
                        boxShadow: '0 8px 32px 0 rgba(3, 136, 166, 0.2)', // Soft primary shadow
                    }}
                >
                    <Typography variant="h4" component="h1" gutterBottom fontWeight="bold" sx={{ color: '#0388a6' }}>
                        Aramıza Katıl 🚀
                    </Typography>
                    <Typography variant="body1" sx={{ color: '#14231d', mb: 3 }}>
                        Haber dünyasına dalmak için hesap oluştur.
                    </Typography>

                    {success ? (
                        <Alert severity="success" sx={{ mb: 2 }}>
                            ✅ Kayıt Başarılı! Giriş ekranına yönlendiriliyorsun...
                        </Alert>
                    ) : (
                        <form onSubmit={handleRegister}>
                            <TextField
                                label="Kullanıcı Adı Seç"
                                variant="outlined"
                                fullWidth
                                margin="normal"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                                InputProps={{
                                    style: { color: '#14231d' }
                                }}
                                InputLabelProps={{
                                    style: { color: '#0388a6' }
                                }}
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        '& fieldset': { borderColor: '#8bd1c3' },
                                        '&:hover fieldset': { borderColor: '#0388a6' },
                                        '&.Mui-focused fieldset': { borderColor: '#0388a6' },
                                    }
                                }}
                            />
                            <TextField
                                label="Güçlü Bir Şifre Belirle"
                                type="password"
                                variant="outlined"
                                fullWidth
                                margin="normal"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                InputProps={{
                                    style: { color: '#14231d' }
                                }}
                                InputLabelProps={{
                                    style: { color: '#0388a6' }
                                }}
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        '& fieldset': { borderColor: '#8bd1c3' },
                                        '&:hover fieldset': { borderColor: '#0388a6' },
                                        '&.Mui-focused fieldset': { borderColor: '#0388a6' },
                                    }
                                }}
                            />

                            {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

                            <Button
                                type="submit"
                                variant="contained"
                                fullWidth
                                size="large"
                                disabled={loading}
                                sx={{
                                    mt: 3,
                                    mb: 2,
                                    bgcolor: '#0388a6',
                                    '&:hover': { bgcolor: '#026e85' },
                                    fontWeight: 'bold'
                                }}
                            >
                                {loading ? <CircularProgress size={24} color="inherit" /> : 'Kayıt Ol'}
                            </Button>
                        </form>
                    )}

                    <Typography variant="body2" sx={{ color: '#546e7a', mt: 2 }}>
                        Zaten hesabın var mı?{' '}
                        <Link component={RouterLink} to="/" sx={{ color: '#0388a6', fontWeight: 'bold' }} underline="hover">
                            Giriş Yap
                        </Link>
                    </Typography>

                    <Typography variant="caption" display="block" sx={{ mt: 4, color: '#b0bec5' }}>
                        © 2024 Personal Content Curator
                    </Typography>
                </Paper>
            </Container>
        </Box>
    );
};

export default Register;