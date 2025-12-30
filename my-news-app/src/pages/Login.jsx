// src/pages/Login.jsx
import React, { useState } from 'react';
import AuthService from '../services/AuthService';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { Box, Button, TextField, Typography, Container, Paper, Link, Alert } from '@mui/material';

const Login = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");

        try {
            await AuthService.login(username, password);
            navigate("/news");
        } catch (err) {
            setError("Giriş başarısız! Bilgileri kontrol et.");
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
                        Hoş Geldin 👋
                    </Typography>
                    <Typography variant="body1" sx={{ color: '#14231d', mb: 3 }}>
                        Haberlere erişmek için giriş yapmalısın.
                    </Typography>

                    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

                    <form onSubmit={handleLogin}>
                        <TextField
                            label="Kullanıcı Adı"
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
                            label="Şifre"
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

                        <Button
                            type="submit"
                            variant="contained"
                            fullWidth
                            size="large"
                            sx={{
                                mt: 3,
                                mb: 2,
                                bgcolor: '#0388a6',
                                '&:hover': { bgcolor: '#026e85' },
                                fontWeight: 'bold'
                            }}
                        >
                            Giriş Yap
                        </Button>
                    </form>

                    <Typography variant="body2" sx={{ color: '#546e7a', mt: 2 }}>
                        Hesabın yok mu?{' '}
                        <Link component={RouterLink} to="/register" sx={{ color: '#0388a6', fontWeight: 'bold' }} underline="hover">
                            Hemen Kayıt Ol
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

export default Login;