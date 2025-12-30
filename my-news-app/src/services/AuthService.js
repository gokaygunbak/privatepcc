// src/services/AuthService.js
import axios from 'axios';

// Tüm istekler Gateway kapısına (8080) gidecek!
const API_URL = "http://localhost:8080/api/auth/";

const register = async (username, password) => {
    try {
        // Backend'deki endpointin "/register" olduğunu varsayıyoruz
        const response = await axios.post(API_URL + "register", {
            username,
            password
        });
        return response.data;
    } catch (error) {
        throw error;
    }
};
const login = async (username, password) => {
    try {
        const response = await axios.post(API_URL + "login", {
            username,
            password
        });
        // Eğer token geldiyse, bunu tarayıcının kasasına (localStorage) kilitle
        if (response.data && response.data.token) {
            console.log("Login Başarılı! Sunucudan gelen yanıt:", response.data);
            localStorage.setItem("user_token", response.data.token);
            if (response.data.userId) {
                console.log("User ID kaydediliyor:", response.data.userId); // DEBUG
                localStorage.setItem("user_id", response.data.userId); // ID'yi de sakla
            } else {
                console.error("UYARI: Sunucu yanıtında 'userId' bulunamadı!", response.data);
            }
        }

        return response.data;
    } catch (error) {
        throw error;
    }
};

const logout = () => {
    localStorage.removeItem("user_token");
    localStorage.removeItem("user_id");
};

const getCurrentToken = () => {
    return localStorage.getItem("user_token");
};

const getCurrentUserId = () => {
    return localStorage.getItem("user_id");
};

export default {
    register,
    login,
    logout,
    getCurrentToken,
    getCurrentUserId
};