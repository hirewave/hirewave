# ==========================================
# Docker Monitoring Stack Permissions Fix
# Run these from the directory containing your docker-compose.yml
# ==========================================

# Fix Loki permissions (Runs as UID 10001)
sudo chown -R 10001:10001 ./infrastructure/loki/

# Fix Prometheus permissions (Runs as UID 65534 - 'nobody')
sudo chown -R 65534:65534 ./infrastructure/prometheus/

# Fix Grafana permissions (Runs as UID 472)
# (Run this if Grafana ever crashes or fails to save dashboards)
sudo chown -R 472:472 ./infrastructure/grafana/