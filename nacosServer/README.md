# Nacos Server Management Application

Bu Spring Boot uygulaması, Nacos Server'ın durumunu kontrol etmek ve yönetmek için kullanılır.

## ⚠️ ÖNEMLİ NOT

**Bu uygulama Nacos Server'ı başlatmaz!** Nacos Server ayrı bir Docker container olarak çalışır.

## 🚀 Nacos Server'ı Başlatma

### Docker Compose ile (Önerilen)

```bash
# Proje kök dizininden
docker-compose up -d nacos
```

### Docker ile Manuel

```bash
docker run -d \
  --name nacos-server \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  nacos/nacos-server:v2.3.0
```

## 🌐 Nacos Console

- **URL:** http://localhost:8848/nacos
- **Kullanıcı adı:** `nacos`
- **Şifre:** `nacos`

## 📋 Bu Uygulamanın Kullanımı

Bu Spring Boot uygulaması şu endpoint'leri sağlar:

### Health Check
```bash
GET http://localhost:8762/api/nacos/health
```

Nacos Server'ın çalışıp çalışmadığını kontrol eder.

### Server Info
```bash
GET http://localhost:8762/api/nacos/info
```

Nacos Server bilgilerini getirir.

## 🔧 Konfigürasyon

`application.properties` dosyasında:
- Port: 8762
- Nacos Server URL: http://localhost:8848

## 📚 Daha Fazla Bilgi

- Nacos Server'ı başlatmak için: `docker-compose up -d nacos`
- Nacos Console: http://localhost:8848/nacos
- Detaylı kurulum: Proje kök dizinindeki `NACOS_SERVER_KURULUM.md`

---

**NOT:** Bu uygulama sadece Nacos Server'ı yönetmek içindir. Gerçek Nacos Server Docker container olarak çalışır.
