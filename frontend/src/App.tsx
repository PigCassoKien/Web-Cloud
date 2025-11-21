// src/App.tsx – PHIÊN BẢN HOÀN CHỈNH, KHÔNG LỖI NỮA
import { useState, useEffect } from 'react';
import { ConfigProvider, message } from 'antd';
import RegisterPage from './components/RegisterPage';
import LoginPage from './components/LoginPage';
import Dashboard from './components/Dashboard';
import { userService } from './services/userService'; // ← THÊM DÒNG NÀY
import './App.css';

type AppState = 'login' | 'register' | 'dashboard';

function App() {
  const [currentView, setCurrentView] = useState<AppState>('login');
  const [currentUser, setCurrentUser] = useState<any>(null);

  // Kiểm tra user đã login chưa (khi reload trang)
  useEffect(() => {
    const user = userService.getCurrentUser();
    if (user) {
      setCurrentUser(user);
      setCurrentView('dashboard');
    }
  }, []);

  const handleLoginSuccess = (userFromBackend: any) => {
    // userFromBackend phải có dạng: { userId: "UUID thật", email: "...", name?: "..." }
    userService.setUserInfo(userFromBackend.userId, userFromBackend.email, userFromBackend.name || '');
    setCurrentUser(userFromBackend);
    setCurrentView('dashboard');
    message.success('Đăng nhập thành công!');
  };

  const handleRegisterSuccess = (userFromBackend: any) => {
    userService.setUserInfo(userFromBackend.userId, userFromBackend.email, userFromBackend.name || '');
    setCurrentUser(userFromBackend);
    setCurrentView('dashboard');
    message.success('Đăng ký thành công! Chào mừng bạn đến SmartQueue 🎉');
  };

  const handleLogout = () => {
    userService.logout();
    setCurrentUser(null);
    setCurrentView('login');
    message.info('Đã đăng xuất');
  };

  const renderCurrentView = () => {
    switch (currentView) {
      case 'register':
        return (
          <RegisterPage
            onRegisterSuccess={handleRegisterSuccess}
            onSwitchToLogin={() => setCurrentView('login')}
          />
        );
      case 'dashboard':
        return currentUser ? (
          <Dashboard user={currentUser} onLogout={handleLogout} />
        ) : null;
      case 'login':
      default:
        return (
          <LoginPage
            onLoginSuccess={handleLoginSuccess}
            onSwitchToRegister={() => setCurrentView('register')}
          />
        );
    }
  };

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#667eea',
          borderRadius: 8,
        },
      }}
    >
      <div className="App">
        {renderCurrentView()}
      </div>
    </ConfigProvider>
  );
}

export default App;