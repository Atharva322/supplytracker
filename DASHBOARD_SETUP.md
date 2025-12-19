# SupplyTracker Dashboard & Alerting Setup Guide

## Quick Start

### 1. Apply Configuration Changes

Restart Prometheus to load the new alert rules:

```powershell
cd C:\Users\athar\Desktop\supplytracker
docker-compose restart prometheus
```

Verify alerts are loaded:
```powershell
# Open Prometheus and go to Alerts tab
Start-Process http://localhost:9090/alerts
```

### 2. Import Grafana Dashboard

1. **Open Grafana**: http://localhost:3000 (admin/admin123)

2. **Import Dashboard**:
   - Click the `+` icon in the left sidebar
   - Select "Import dashboard"
   - Click "Upload JSON file"
   - Select: `C:\Users\athar\Desktop\supplytracker\monitoring\supplytracker-dashboard.json`
   - Select Prometheus datasource: "Prometheus"
   - Click "Import"

3. **View Your Dashboard**:
   - Your "SupplyTracker Health Dashboard" is now live!
   - Set refresh to 10s for real-time monitoring

## Dashboard Panels Explained

### 🔴 Critical Monitoring Panels

#### 1. CSV Import Performance (95th Percentile)
- **What it shows**: How long CSV imports take for 95% of requests
- **Why it matters**: Farmers uploading bulk product data need this to be fast
- **Alert threshold**: > 5 seconds
- **Action if alert fires**: 
  - Check if large files are being imported
  - Review MongoDB indexes on products collection
  - Consider pagination for large imports

#### 2. MongoDB Query Performance
- **What it shows**: Average query time and queries per second
- **Why it matters**: Slow queries = slow application
- **Alert threshold**: > 1 second average
- **Action if alert fires**:
  - Check MongoDB logs for slow queries
  - Add indexes: `db.products.createIndex({name: 1})`
  - Review ProductRepository queries

#### 3. API Error Rate
- **What it shows**: Percentage of failed requests (5xx errors)
- **Why it matters**: Indicates production bugs affecting users
- **Alert threshold**: > 5%
- **Action if alert fires**:
  - Check Loki logs: `{app="supplytracker-backend"} |= "ERROR"`
  - Review GlobalExceptionHandler logs
  - Check MongoDB connection status

#### 4. Redis Cache Hit Rate
- **What it shows**: How often data is served from cache vs database
- **Why it matters**: Higher hit rate = faster responses, less DB load
- **Target**: > 70%
- **Action if low**:
  - Increase cache TTL in application.properties
  - Add more @Cacheable annotations
  - Verify Redis is running: `docker ps | findstr redis`

### 📊 Performance Monitoring Panels

#### 5. API Request Rate by Endpoint
- Shows which endpoints are most used
- Helps identify hotspots for optimization

#### 6. JVM Heap Memory Usage
- Monitors Java memory consumption
- Alert if > 85% to prevent OutOfMemoryError

#### 7. Top 5 Slowest API Endpoints
- Identifies which endpoints need optimization
- Focus your development efforts here

#### 8. MongoDB Connection Pool Usage
- Ensures you're not running out of database connections
- Alert if > 90% used

### 🔒 Security & Health Panels

#### 9. Failed Login Attempts
- **What it shows**: Rate of failed login attempts
- **Why it matters**: Detect brute force attacks
- **Alert threshold**: > 0.1/sec (6 per minute)
- **Action**: Review Auth logs, consider rate limiting

#### 10. Application Uptime
- Shows if Spring Boot is responding
- Instant alert if application goes down

#### 11. Total Requests (Last Hour)
- Business metric: How many requests you're serving
- Track growth over time

#### 12. Active Users (Unique IPs)
- How many users are currently using the system
- Capacity planning metric

## Alert Rules Configured

### Active Alerts (9 Total)

| Alert Name | Severity | Threshold | Response Time |
|------------|----------|-----------|---------------|
| SlowCSVImport | Warning | > 5s | 2 minutes |
| SlowMongoDBQueries | Warning | > 1s avg | 3 minutes |
| HighAPIErrorRate | Critical | > 5% | 2 minutes |
| LowCacheHitRate | Info | < 50% | 5 minutes |
| HighJVMMemoryUsage | Warning | > 85% | 5 minutes |
| SlowAPIResponses | Warning | > 500ms | 3 minutes |
| MongoDBConnectionPoolExhausted | Critical | > 90% | 2 minutes |
| HighFailedLoginRate | Warning | > 0.1/s | 3 minutes |
| ApplicationDown | Critical | App offline | 1 minute |

### Alert States
- **Inactive** (Green): Everything is normal
- **Pending** (Yellow): Condition met but waiting for "for" duration
- **Firing** (Red): Alert is active - take action!

## Testing Alerts

### Test 1: Trigger High Error Rate Alert
```powershell
# Make multiple requests to a non-existent endpoint
for ($i=1; $i -le 20; $i++) { 
    curl http://localhost:8080/api/products/nonexistent -UseBasicParsing -ErrorAction SilentlyContinue
}
```
Check alert: http://localhost:9090/alerts (should go yellow → red)

### Test 2: Monitor CSV Import
1. Import a large CSV file in your app
2. Watch the "CSV Import Performance" panel in real-time
3. See if it stays under 5-second threshold

### Test 3: Check Cache Effectiveness
```powershell
# Hit the same product multiple times
$id = "some-product-id"
for ($i=1; $i -le 10; $i++) { 
    curl http://localhost:8080/api/products/$id
}
```
Check cache hit rate gauge - should increase

## Setting Up Alertmanager (Optional)

To receive alert notifications (email, Slack, SMS):

### 1. Add Alertmanager to docker-compose.yml
```yaml
  alertmanager:
    image: prom/alertmanager:latest
    container_name: supplytracker-alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    networks:
      - supplytracker-network
```

### 2. Create alertmanager.yml
```yaml
global:
  resolve_timeout: 5m

route:
  receiver: 'email'
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: 'email'
    email_configs:
      - to: 'your-email@example.com'
        from: 'alerts@supplytracker.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'your-email@gmail.com'
        auth_password: 'your-app-password'
```

### 3. Update prometheus.yml
```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

## Best Practices

### Daily Monitoring
1. **Morning Check**: Open dashboard, verify all panels are green
2. **Review Alerts**: Check http://localhost:9090/alerts for any pending/firing
3. **Check Trends**: Are response times trending up? Memory usage growing?

### Weekly Review
1. **Slow Endpoints**: Review "Top 5 Slowest" - can they be optimized?
2. **Cache Performance**: Is hit rate consistent? Should you adjust TTL?
3. **Error Patterns**: Any specific endpoints failing more than others?

### Before Deployment
1. **Baseline Metrics**: Note current performance metrics
2. **Deploy Changes**: Update application
3. **Compare Metrics**: Did performance improve or degrade?
4. **Rollback Plan**: If metrics degrade significantly, rollback

## Troubleshooting

### Dashboard Shows "No Data"
```powershell
# Check Prometheus is scraping
curl http://localhost:9090/api/v1/targets

# Check Spring Boot metrics endpoint
curl http://localhost:8080/actuator/prometheus
```

### Alerts Not Firing
```powershell
# Verify alert rules are loaded
docker logs supplytracker-prometheus | Select-String "alert"

# Check alert status
Start-Process http://localhost:9090/alerts
```

### Cache Hit Rate Shows 0%
```powershell
# Check Redis is running
docker ps | findstr redis

# Test Redis connection
docker exec -it supplytracker-redis redis-cli ping
# Should return: PONG

# Check if cache entries exist
docker exec -it supplytracker-redis redis-cli
> KEYS products::*
```

## Real-World Scenarios

### Scenario 1: Farmer Reports Slow CSV Import
1. Open dashboard → Check "CSV Import Performance" panel
2. If showing > 5s: Check "MongoDB Query Performance"
3. If MongoDB slow: Add indexes or optimize queries
4. Monitor improvement in real-time on dashboard

### Scenario 2: Application Feels Sluggish
1. Check "API Request Rate" - is there unusual traffic?
2. Check "Top 5 Slowest Endpoints" - which need optimization?
3. Check "JVM Heap Memory" - running low on memory?
4. Check "MongoDB Connection Pool" - exhausted?

### Scenario 3: Security Concern
1. Check "Failed Login Attempts" - any spikes?
2. Check Loki logs: `{app="supplytracker-backend"} |= "login" |= "failed"`
3. Identify suspicious IPs and consider blocking

## Next Steps

1. ✅ Restart Prometheus to load alert rules
2. ✅ Import dashboard to Grafana
3. ✅ Test alerts by triggering errors
4. ✅ Monitor dashboard during normal usage
5. ⏳ Set up Alertmanager for notifications (optional)
6. ⏳ Create custom dashboards for business metrics
7. ⏳ Document team's response procedures for each alert

## Resources

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000
- **Alert Rules**: C:\Users\athar\Desktop\supplytracker\monitoring\alert-rules.yml
- **Dashboard JSON**: C:\Users\athar\Desktop\supplytracker\monitoring\supplytracker-dashboard.json
- **PromQL Guide**: https://prometheus.io/docs/prometheus/latest/querying/basics/
- **Grafana Docs**: https://grafana.com/docs/grafana/latest/

---

**Remember**: The goal is proactive monitoring - fixing issues before users report them!
