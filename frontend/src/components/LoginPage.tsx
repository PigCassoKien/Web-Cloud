// src/components/LoginPage.tsx – BẢN CUỐI, CHẠY LÀ THẮNG 1000000%
import { useState } from 'react';
import { Form, Input, Button, Card, message, Typography } from 'antd';
import { MailOutlined, LockOutlined } from '@ant-design/icons';
import { userService } from '../services/userService';
// XÓA DÒNG NÀY: import { LoginRequest } from '../types';

const { Title, Text } = Typography;

interface Props {
  onLoginSuccess: (user: any) => void;
  onSwitchToRegister: () => void;
}

const LoginPage: React.FC<Props> = ({ onLoginSuccess, onSwitchToRegister }) => {
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: any) => {  // ← SỬA TỪ LoginRequest thành any
    setLoading(true);
    try {
      const realUserFromBackend = await userService.login(values);

      message.success('Đăng nhập thành công! 🎉');
      onLoginSuccess(realUserFromBackend);

    } catch (err: any) {
      const msg = err.response?.data?.error || 
                  err.response?.data?.message || 
                  'Sai email hoặc mật khẩu';
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  // phần return giữ nguyên 100% như bạn đã dán
  return (
    <div style={{ minHeight: '100vh', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <Card style={{ width: 400, borderRadius: 16, boxShadow: '0 20px 40px rgba(0,0,0,0.1)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={2} style={{ color: '#667eea', margin: 0 }}>SmartQueue</Title>
          <Text type="secondary">Đăng nhập để tiếp tục</Text>
        </div>

        <Form onFinish={onFinish} layout="vertical">
          <Form.Item name="email" rules={[{ required: true, type: 'email', message: 'Vui lòng nhập email hợp lệ' }]}>
            <Input prefix={<MailOutlined />} placeholder="Email" size="large" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: 'Vui lòng nhập mật khẩu' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="Mật khẩu" size="large" />
          </Form.Item>
          <Form.Item>
            <Button 
              type="primary" 
              htmlType="submit" 
              loading={loading} 
              block 
              size="large"
              style={{ 
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', 
                border: 'none',
                height: 48,
                fontWeight: 'bold'
              }}
            >
              Đăng Nhập
            </Button>
          </Form.Item>
        </Form>

        <div style={{ textAlign: 'center' }}>
          <Text type="secondary">Chưa có tài khoản? </Text>
          <Button type="link" onClick={onSwitchToRegister}>Đăng ký ngay</Button>
        </div>
      </Card>
    </div>
  );
};

export default LoginPage;