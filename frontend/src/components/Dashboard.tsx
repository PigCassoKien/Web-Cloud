import { useState, useEffect } from 'react';
import {
  Layout, Card, Button, List, Badge, Statistic,
  Row, Col, message, Typography, Space, Tag, Modal, Progress
} from 'antd';
import {
  ClockCircleOutlined, TeamOutlined, ThunderboltOutlined,
  BellOutlined, UserOutlined, LogoutOutlined, ReloadOutlined
} from '@ant-design/icons';
import { queueService } from '../services/queueService';
import { userService } from '../services/userService';
import { QueueInfo, Ticket, EtaResponse } from '../types';

const { Header, Content } = Layout;
const { Title, Text } = Typography;

interface MyTicket extends Ticket {
  estimatedWaitMinutes?: number;
  remainingMinutes?: number | null;
}

interface DashboardProps {
  user: any;
  onLogout: () => void;
}

const Dashboard: React.FC<DashboardProps> = ({ user, onLogout }) => {
  const [queues, setQueues] = useState<QueueInfo[]>([]);
  const [myTickets, setMyTickets] = useState<MyTicket[]>([]);
  const [loading, setLoading] = useState(false);
  const [joining, setJoining] = useState<string | null>(null);

  const currentUserFromProps = user;
  let storedUser: any = null;
  try {
    const raw = localStorage.getItem('currentUser');
    storedUser = raw ? JSON.parse(raw) : null;
  } catch {}
  const currentUser = currentUserFromProps || storedUser || userService.getCurrentUser();
  const currentUserId = currentUser?.userId ?? null;
  const currentUserEmail = currentUser?.email ?? localStorage.getItem('userEmail') ?? 'Unknown';

  useEffect(() => {
    if (!currentUserId) {
      message.warning('Please login to continue');
      onLogout();
    } else {
      refreshAll();
    }
  }, [currentUserId, onLogout]);

  const refreshAll = async () => {
    setLoading(true);
    try {
      await loadQueues();
      await updateTicketsFromCache();
    } finally {
      setLoading(false);
    }
  };

  const loadQueues = async () => {
    try {
      const data = await queueService.getQueues();
      setQueues(data);
    } catch (e: any) {
      message.error(e.response?.data?.message || 'Failed to load queues');
    }
  };

  // Load tickets from backend then map ETA from local cache only (no API calls)
  const loadMyTickets = async () => {
    if (!currentUserId) return;
    try {
      const tickets = await userService.getMyTickets(currentUserId);
      const mapped = tickets.map(t => {
        const eta = queueService.getCachedEta(t.ticketId);
        return {
          ...t,
          estimatedWaitMinutes: eta?.remainingMinutes ?? eta?.estimatedWaitMinutes ?? null,
          remainingMinutes: eta?.remainingMinutes ?? null
        };
      });
      setMyTickets(mapped);
    } catch (e: any) {
      message.error(e.response?.data?.message || 'Failed to load tickets');
    }
  };

  // Recompute remainingMinutes from local cache for existing myTickets
  const updateTicketsFromCache = async () => {
    if (!currentUserId) return;
    await loadMyTickets();
  };

  const joinQueue = async (queueId: string) => {
    if (!currentUserId) return message.error('User not logged in');
    setJoining(queueId);
    try {
      const ticket = await queueService.joinQueue(queueId, currentUserId);
      // Call /eta/track only once here to initialize tracking; then rely on cache afterward
      const eta = await queueService.trackETA(
        queueId,
        ticket.ticketId,
        ticket.position,
        currentUserEmail,
        true
      );
      message.success(`Joined successfully! Your position: ${ticket.position}`);
      showTicketModal(ticket, eta);
      setMyTickets(prev => [
        ...prev,
        {
          ...ticket,
          estimatedWaitMinutes: eta.remainingMinutes ?? eta.estimatedWaitMinutes ?? null,
          remainingMinutes: eta.remainingMinutes ?? null
        }
      ]);
    } catch (e: any) {
      message.error(e.response?.data?.message || 'Failed to join queue');
    } finally {
      setJoining(null);
    }
  };

  const showTicketModal = (ticket: Ticket, eta: EtaResponse) => {
    Modal.info({
      title: '🎫 Your Queue Ticket',
      width: 520,
      centered: true,
      maskClosable: true,
      footer: null,
      content: (
        <Card bordered={false}>
          <Statistic
            title="Your Position"
            value={ticket.position}
            prefix={<TeamOutlined />}
            suffix="in line"
            valueStyle={{ fontSize: 36 }}
          />
          <br />
          <Statistic
            title="Smart ETA"
            value={eta.remainingMinutes ?? eta.estimatedWaitMinutes}
            prefix={<ClockCircleOutlined />}
            suffix="minutes"
            valueStyle={{ fontSize: 32 }}
          />
          <br />
          <Text type="secondary">
            Ticket ID: <Text strong copyable>{ticket.ticketId}</Text>
            <br />
            Email: <Text strong>{currentUserEmail}</Text>
          </Text>
          <div style={{ marginTop: 24 }}>
            <Progress
              percent={Math.round((1 - ticket.position / (ticket.position + 20)) * 100)}
              status="active"
              format={p => `${p}%`}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              Estimated progress
            </Text>
          </div>
        </Card>
      )
    });
  };

  const logout = () => {
    userService.logout();
    message.success('Logged out');
    onLogout();
  };

  const getQueueStatusColor = (waitingCount: number): 'green' | 'orange' | 'red' => {
    if (waitingCount <= 5) return 'green';
    if (waitingCount <= 15) return 'orange';
    return 'red';
  };

  return (
    <Layout style={{ minHeight: '100vh', background: '#f0f2f5' }}>
      <Header style={{
        background: 'linear-gradient(135deg,#667eea,#764ba2)',
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <Title level={3} style={{ color: 'white', margin: 0 }}>SmartQueue</Title>
        <Space>
          <Text style={{ color: 'white' }}><UserOutlined /> {currentUserEmail}</Text>
          <Button
            icon={<ReloadOutlined />}
            onClick={refreshAll}
            loading={loading}
            style={{ border: 'none', background: 'rgba(255,255,255,0.2)', color: 'white' }}
          >
            Refresh
          </Button>
          <Button
            icon={<LogoutOutlined />}
            onClick={logout}
            style={{ border: 'none', background: 'rgba(255,255,255,0.2)', color: 'white' }}
          >
            Logout
          </Button>
        </Space>
      </Header>

      <Content style={{ padding: 24 }}>
        <Row gutter={[24, 24]}>
          <Col xs={24} lg={16}>
            <Card
              title={<Space><TeamOutlined /> Available Queues <Badge count={queues.length} /></Space>}
              loading={loading}
            >
              {queues.length === 0 ? (
                <Text type="secondary">No queues available.</Text>
              ) : (
                <List
                  dataSource={queues}
                  renderItem={queue => (
                    <List.Item
                      actions={[
                        <Button
                          key="join"
                          type="primary"
                          icon={<ThunderboltOutlined />}
                          loading={joining === queue.queueId}
                          onClick={() => joinQueue(queue.queueId)}
                        >
                          Join
                        </Button>
                      ]}
                    >
                      <List.Item.Meta
                        title={
                          <Space>
                            <Text strong>{queue.name}</Text>
                            <Tag color={getQueueStatusColor(queue.currentWaitingCount)}>
                              {queue.currentWaitingCount} waiting
                            </Tag>
                          </Space>
                        }
                        description={
                          <Space direction="vertical" size={2}>
                            <Text>{queue.description || 'No description'}</Text>
                            <Space>
                              <Tag icon={<ClockCircleOutlined />} color="blue">
                                ~{queue.averageServiceTimeMinutes} min avg
                              </Tag>
                              <Tag color={queue.isActive ? 'green' : 'red'}>
                                {queue.isActive ? 'Open' : 'Closed'}
                              </Tag>
                            </Space>
                          </Space>
                        }
                      />
                    </List.Item>
                  )}
                />
              )}
            </Card>
          </Col>

          <Col xs={24} lg={8}>
            <Card title={<Space><BellOutlined /> My Tickets <Badge count={myTickets.length} /></Space>}>
              {myTickets.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '32px 0' }}>
                  <BellOutlined style={{ fontSize: 40, color: '#d9d9d9' }} />
                  <div>No active tickets</div>
                </div>
              ) : (
                <List
                  dataSource={myTickets}
                  renderItem={t => (
                    <List.Item>
                      <List.Item.Meta
                        title={<Text strong>Position {t.position}</Text>}
                        description={
                          <Space direction="vertical" size={0}>
                            <Tag color="processing">{t.status || 'Waiting'}</Tag>
                            <Text type="secondary">
                              ETA: {t.remainingMinutes != null ? `${t.remainingMinutes} min` : '—'}
                            </Text>
                          </Space>
                        }
                      />
                    </List.Item>
                  )}
                />
              )}
            </Card>

            <Card title="Stats" style={{ marginTop: 16 }}>
              <Row gutter={12}>
                <Col span={12}>
                  <Statistic title="Avg Wait" value={8.5} precision={1} suffix="min" />
                </Col>
                <Col span={12}>
                  <Statistic title="Served Today" value={245} />
                </Col>
              </Row>
            </Card>
          </Col>
        </Row>
      </Content>
    </Layout>
  );
};

export default Dashboard;
