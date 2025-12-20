# Nacos Server Başlatma Kılavuzu

## 🎯 Hızlı Başlangıç

### 1. Docker Compose ile (En Kolay)

```bash
# Proje kök dizininden
docker-compose up -d nacos
```

### 2. Docker ile Manuel

```bash
docker run -d \
  --name nacos-server \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  nacos/nacos-server:v2.3.0
```

### 3. Durumu Kontrol Et

```bash
# Container'ın çalıştığını kontrol et
docker ps | grep nacos

# Logları görüntüle
docker logs nacos-server
```

## ✅ Başarı Kontrolü

1. Tarayıcıda açın: http://localhost:8848/nacos
2. Giriş yapın:
   - Kullanıcı adı: `nacos`
   - Şifre: `nacos`
3. **Services** → **Service List** menüsünde servislerin kayıt olduğunu kontrol edin

## 🛑 Durdurma

```bash
# Docker Compose ile
docker-compose stop nacos

# Docker ile
docker stop nacos-server
```

## 🔄 Yeniden Başlatma

```bash
# Docker Compose ile
docker-compose restart nacos

# Docker ile
docker restart nacos-server
```

## 🗑️ Kaldırma

```bash
# Docker Compose ile
docker-compose down nacos

# Docker ile
docker stop nacos-server
docker rm nacos-server
```

