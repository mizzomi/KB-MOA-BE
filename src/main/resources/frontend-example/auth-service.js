/**
 * 프론트엔드 인증 서비스 예제 (JavaScript/React)
 * Axios를 사용한 토큰 관리 및 자동 갱신
 */

import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// Axios 인스턴스 생성
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // Cookie 전송 활성화 (Refresh Token용)
});

// Access Token 저장소 (메모리)
let accessToken = null;

/**
 * Access Token 설정
 */
export const setAccessToken = (token) => {
  accessToken = token;
  if (token) {
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete apiClient.defaults.headers.common['Authorization'];
  }
};

/**
 * 로그인
 */
export const login = async (email, password) => {
  try {
    const response = await apiClient.post('/public/login', { email, password });
    const { accessToken: token, user, expiresIn } = response.data;
    
    // Access Token 저장
    setAccessToken(token);
    
    // 만료 시간 저장 (선택사항)
    const expiryTime = Date.now() + (expiresIn * 1000);
    localStorage.setItem('tokenExpiry', expiryTime);
    
    // 사용자 정보 저장 (선택사항)
    localStorage.setItem('user', JSON.stringify(user));
    
    return { success: true, user };
  } catch (error) {
    console.error('Login failed:', error);
    return { success: false, error: error.response?.data || 'Login failed' };
  }
};

/**
 * Access Token 갱신
 */
export const refreshAccessToken = async () => {
  try {
    // Refresh Token은 Cookie에 있으므로 별도 전송 불필요
    const response = await apiClient.post('/public/auth/refresh');
    const { accessToken: newToken, expiresIn } = response.data;
    
    // 새로운 Access Token 설정
    setAccessToken(newToken);
    
    // 만료 시간 업데이트
    const expiryTime = Date.now() + (expiresIn * 1000);
    localStorage.setItem('tokenExpiry', expiryTime);
    
    return newToken;
  } catch (error) {
    console.error('Token refresh failed:', error);
    // Refresh Token도 만료된 경우 로그인 페이지로 리다이렉트
    logout();
    window.location.href = '/login';
    return null;
  }
};

/**
 * 로그아웃
 */
export const logout = async () => {
  try {
    await apiClient.post('/auth/logout');
  } catch (error) {
    console.error('Logout error:', error);
  } finally {
    // 로컬 상태 초기화
    setAccessToken(null);
    localStorage.removeItem('tokenExpiry');
    localStorage.removeItem('user');
  }
};

/**
 * Request Interceptor - 요청 전 토큰 만료 확인
 */
apiClient.interceptors.request.use(
  async (config) => {
    // public 엔드포인트는 토큰 체크 스킵
    if (config.url?.includes('/public/')) {
      return config;
    }
    
    // 토큰 만료 시간 확인
    const tokenExpiry = localStorage.getItem('tokenExpiry');
    if (tokenExpiry) {
      const now = Date.now();
      const expiryTime = parseInt(tokenExpiry);
      
      // 만료 1분 전에 자동 갱신
      if (expiryTime - now < 60000) {
        console.log('Token expiring soon, refreshing...');
        await refreshAccessToken();
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response Interceptor - 401 에러 처리
 */
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // 401 에러이고 토큰 만료인 경우
    if (error.response?.status === 401 && 
        error.response?.headers['token-expired'] === 'true' &&
        !originalRequest._retry) {
      
      originalRequest._retry = true;
      
      try {
        // Access Token 갱신
        const newToken = await refreshAccessToken();
        
        if (newToken) {
          // 원래 요청 재시도
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // Refresh 실패시 로그인 페이지로
        logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

/**
 * 현재 사용자 정보 조회
 */
export const getCurrentUser = async () => {
  try {
    const response = await apiClient.get('/auth/me');
    return response.data;
  } catch (error) {
    console.error('Failed to get current user:', error);
    return null;
  }
};

/**
 * API 호출 예제
 */
export const fetchUserReservations = async (tripId) => {
  try {
    const response = await apiClient.get(`/reservations/${tripId}`);
    return response.data;
  } catch (error) {
    console.error('Failed to fetch reservations:', error);
    throw error;
  }
};

// 페이지 로드시 토큰 확인
export const initializeAuth = () => {
  const tokenExpiry = localStorage.getItem('tokenExpiry');
  
  if (tokenExpiry) {
    const now = Date.now();
    const expiryTime = parseInt(tokenExpiry);
    
    if (expiryTime > now) {
      // 토큰이 아직 유효하면 갱신 시도
      refreshAccessToken().catch(() => {
        // 실패시 로그인 페이지로
        logout();
        window.location.href = '/login';
      });
    } else {
      // 토큰 만료
      logout();
      window.location.href = '/login';
    }
  }
};

export default apiClient;
