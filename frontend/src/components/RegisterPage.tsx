// src/components/RegisterPage.tsx – BẢN CUỐI CÙNG, HOÀN HẢO
import { useState } from 'react';
import { Form, Input, Button, Card, Switch, message, Typography, Space } from 'antd';
import { UserOutlined, MailOutlined, PhoneOutlined, LockOutlined } from '@ant-design/icons';
import { userService } from '../services/userService';
import { CreateUserRequest } from '../types';

const { Title, Text } = Typography;

interface RegisterPageProps {
  onRegisterSuccess: (user: any) => void;
  onSwitchToLogin: () => void;
}

const RegisterPage: React.FC<RegisterPageProps> = ({ onRegisterSuccess, onSwitchToLogin }) => {
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: CreateUserRequest) => {
    // XÓA confirmPassword khỏi request vì backend không cần
    const { confirmPassword, ...registerData } = values;

    setLoading(true);
    try {
      // GỌI ĐĂNG KÝ → userService.register() đã tự lưu UUID thật vào localStorage
      const user = await userService.register(registerData);

      message.success(`Welcome ${user.name || user.email}! 🎉`);
      onRegisterSuccess(user); // Chuyển thẳng sang Dashboard với user thật

    } catch (error: any) {
      const msg = error.response?.data?.message || 
                  error.response?.data?.error || 
                  'Registration failed. Please try again.';
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      minHeight: '100vh', 
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px'
    }}>
      <Card style={{ width: '100%', maxWidth: '500px', borderRadius: '16px', boxShadow: '0 20px 40px rgba(0,0,0,0.1)' }}>
        <div style={{ textAlign: 'center', marginBottom: '30px' }}>
          <Title level={2} style={{ color: '#667eea', margin: 0 }}>🎯 SmartQueue</Title>
          <Text type="secondary">Create your account</Text>
        </div>

        <Form
          name="register"
          onFinish={onFinish}
          layout="vertical"
          initialValues={{
            emailNotificationEnabled: true,
            smsNotificationEnabled: false
          }}
        >
          {/* Tất cả các field giữ nguyên như bạn */}
          <Form.Item name="name" label="Full Name" rules={[{ required: true, min: 2 }]}>
            <Input prefix={<UserOutlined />} placeholder="Enter your full name" size="large" />
          </Form.Item>

          <Form.Item name="email" label="Email Address" rules={[{ required: true, type: 'email' }]}>
            <Input prefix={<MailOutlined />} placeholder="Enter your email" size="large" />
          </Form.Item>

          <Form.Item name="phone" label="Phone Number" rules={[{ required: true, pattern: /^\+?[0-9]{8,15}$/, message: 'Invalid phone number' }]}>
            <Input prefix={<PhoneOutlined />} placeholder="+84333138386" size="large" />
          </Form.Item>

          <Form.Item name="password" label="Password" rules={[{ required: true, min: 8, pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, message: 'Must contain uppercase, lowercase & number' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="Enter password" size="large" />
          </Form.Item>

          <Form.Item name="confirmPassword" label="Confirm Password" dependencies={['password']} rules={[
            { required: true },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('password') === value) return Promise.resolve();
                return Promise.reject('Passwords do not match');
              }
            })
          ]}>
            <Input.Password prefix={<LockOutlined />} placeholder="Confirm password" size="large" />
          </Form.Item>

          <Card size="small" style={{ background: '#f8f9fa', marginBottom: '20px' }}>
            <Title level={5} style={{ margin: '0 0 16px 0' }}>📱 Notification Preferences</Title>
            <Form.Item name="emailNotificationEnabled" valuePropName="checked" style={{ marginBottom: 12 }}>
              <Space><Switch /><Text>Email notifications</Text></Space>
            </Form.Item>
            <Form.Item name="smsNotificationEnabled" valuePropName="checked">
              <Space><Switch /><Text>SMS notifications</Text></Space>
            </Form.Item>
          </Card>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large"
              style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', border: 'none', height: 50, fontWeight: 'bold' }}>
              Create Account
            </Button>
          </Form.Item>

          <div style={{ textAlign: 'center' }}>
            <Text type="secondary">Already have an account? </Text>
            <Button type="link" onClick={onSwitchToLogin}>Sign in here</Button>
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default RegisterPage;