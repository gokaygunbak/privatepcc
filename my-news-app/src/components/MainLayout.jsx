import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    AppBar, Toolbar, Typography, Button, Box, IconButton, Drawer
} from '@mui/material';
import {
    ExitToApp as LogoutIcon,
    FilterList as FilterIcon,
    Home as HomeIcon,
    Person as PersonIcon,
    Check as CheckIcon,
    TrendingUp as TrendingIcon
} from '@mui/icons-material';

const drawerWidth = 240;

const MainLayout = ({ children, title = "🔍 Keşfet" }) => {
    const [mobileOpen, setMobileOpen] = useState(false);
    const navigate = useNavigate();

    const handleLogout = () => {
        localStorage.removeItem("user_token");
        localStorage.removeItem("user_id");
        navigate("/");
    };

    const drawerContent = (
        <Box sx={{ overflow: 'auto', p: 2 }}>
            <Box sx={{ mb: 2 }}>
                <Button
                    fullWidth
                    variant="text"
                    color="inherit"
                    startIcon={<HomeIcon />}
                    onClick={() => navigate("/news")}
                    sx={{ justifyContent: 'flex-start', mb: 1, color: 'text.secondary', '&:hover': { color: 'primary.main', bgcolor: 'rgba(3, 136, 166, 0.1)' } }}
                >
                    Haber Ara
                </Button>
                <Button
                    fullWidth
                    variant="text"
                    color="inherit"
                    startIcon={<TrendingIcon />}
                    onClick={() => navigate("/trends")}
                    sx={{ justifyContent: 'flex-start', mb: 1, color: 'text.secondary', '&:hover': { color: 'primary.main', bgcolor: 'rgba(3, 136, 166, 0.1)' } }}
                >
                    Bu Günlerde
                </Button>
                <Button
                    fullWidth
                    variant="text"
                    color="inherit"
                    startIcon={<PersonIcon />}
                    onClick={() => navigate("/my-feed")}
                    sx={{ justifyContent: 'flex-start', mb: 1, color: 'text.secondary', '&:hover': { color: 'primary.main', bgcolor: 'rgba(3, 136, 166, 0.1)' } }}
                >
                    Size Özel
                </Button>
                <Button
                    fullWidth
                    variant="text"
                    color="inherit"
                    startIcon={<CheckIcon />}
                    onClick={() => navigate("/onboarding")}
                    sx={{ justifyContent: 'flex-start', color: 'text.secondary', '&:hover': { color: 'primary.main', bgcolor: 'rgba(3, 136, 166, 0.1)' } }}
                >
                    İlgi Alanlarım
                </Button>
                <Button
                    fullWidth
                    variant="text"
                    color="inherit"
                    startIcon={<PersonIcon />}
                    onClick={() => navigate("/profile")}
                    sx={{ justifyContent: 'flex-start', color: 'text.secondary', '&:hover': { color: 'primary.main', bgcolor: 'rgba(3, 136, 166, 0.1)' } }}
                >
                    Profilim
                </Button>
            </Box>
        </Box>
    );

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
            {/* AppBar */}
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1, backdropFilter: 'blur(10px)', backgroundColor: 'rgba(255, 255, 255, 0.8)', color: 'text.primary', boxShadow: 1 }}>
                <Toolbar>
                    <IconButton
                        color="inherit"
                        edge="start"
                        onClick={() => setMobileOpen(!mobileOpen)}
                        sx={{ mr: 2, display: { sm: 'none' } }}
                    >
                        <FilterIcon />
                    </IconButton>
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 'bold', letterSpacing: 1, color: 'primary.main' }}>
                        {title}
                    </Typography>
                    <Button color="inherit" onClick={handleLogout} startIcon={<LogoutIcon />}>
                        Çıkış
                    </Button>
                </Toolbar>
            </AppBar>

            {/* Sidebar */}
            <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
                <Drawer
                    variant="temporary"
                    open={mobileOpen}
                    onClose={() => setMobileOpen(!mobileOpen)}
                    ModalProps={{ keepMounted: true }}
                    sx={{
                        display: { xs: 'block', sm: 'none' },
                        '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth, bgcolor: 'background.paper' }
                    }}
                >
                    {drawerContent}
                </Drawer>
                <Drawer
                    variant="permanent"
                    sx={{
                        display: { xs: 'none', sm: 'block' },
                        '& .MuiDrawer-paper': {
                            boxSizing: 'border-box',
                            width: drawerWidth,
                            top: 64,
                            height: 'calc(100% - 64px)',
                            bgcolor: 'background.paper',
                            borderRight: '1px solid rgba(0,0,0,0.08)'
                        }
                    }}
                    open
                >
                    {drawerContent}
                </Drawer>
            </Box>

            {/* Main Content */}
            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                {children}
            </Box>
        </Box>
    );
};

export default MainLayout;
