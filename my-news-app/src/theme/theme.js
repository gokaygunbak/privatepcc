import { createTheme } from '@mui/material/styles';

const theme = createTheme({
    palette: {
        mode: 'light',
        primary: {
            main: '#0388a6', // Pacific Cyan
        },
        secondary: {
            main: '#8bd1c3', // Pearl Aqua
        },
        background: {
            default: '#f4f9f9', // Soft White / Mint Tint
            paper: '#ffffff', // Pure White
        },
        text: {
            primary: '#14231d', // Carbon Black
            secondary: '#546e7a',
        },
    },
    typography: {
        fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
        h1: {
            fontWeight: 700,
            fontSize: '2.5rem',
        },
        h4: {
            fontWeight: 600,
        },
        button: {
            textTransform: 'none', // Buton yazıları büyük harf olmasın, daha modern durur
            fontWeight: 600,
        },
    },
    shape: {
        borderRadius: 12, // Daha yuvarlak köşeler
    },
    components: {
        MuiButton: {
            styleOverrides: {
                root: {
                    borderRadius: '8px',
                    padding: '10px 20px',
                },
            },
        },
        MuiTextField: {
            defaultProps: {
                variant: 'outlined',
                fullWidth: true,
            },
        },
    },
});

export default theme;
