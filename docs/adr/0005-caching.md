# ADR-0005: Ürün okumaları için local cache (Caffeine)

## Durum

Kabul edildi — 17 Kasım 2026

## Bağlam

product-service'te ürün bilgisi (ad, açıklama, fiyat, kategori) seyrek değişiyor ama sık okunuyor:
ürün detayı, ürün listesi ve ileride order-service'in sipariş anında yaptığı çağrılar.
JPA geçişinden (Task 20) sonra her okuma bir veritabanı sorgusu olacak.

Stok adedi ise product-service'in verisi değil (ADR-0004); inventory-service'ten anlık alınıyor
ve sürekli değişiyor. Eski bir stok adedini göstermek yanlış iş kararına (olmayan ürünü satmak) yol açar.

Şu an repository in-memory olduğu için cache'in performans kazancı yok denecek kadar az.
Amaç, soyutlamayı ve invalidation kurallarını şimdi kurmak; kazanç JPA'ya geçince kod değişmeden gelecek.

## Karar

1. **Spring Cache soyutlaması** (`@Cacheable`, `@CacheEvict`, `@Caching`) kullanılır.
   `@EnableCaching` `config/CacheConfig` sınıfında; `CacheManager`'ı Spring Boot auto-configuration kurar,
   ayarlar `application.yaml`'da (`spring.cache.*`).
2. **Sağlayıcı: Caffeine** (aynı JVM içinde, heap'te local cache).
3. **Cache'lenenler:**
    - `getById(id)` → `products` cache'i, anahtar `id`
    - `getAll(limit)` → `productLists` cache'i, anahtar `limit`
4. **Cache'lenmeyen:** `getByIdWithInventory(id)`. Stok her istekte inventory-service'ten taze alınır.
5. **Invalidation (evict):**
    - `update(id)` ve `delete(id)` → `products`'tan o id + `productLists`'in tamamı
    - `create` → `productLists`'in tamamı (yeni id `products`'ta zaten yok)
    - Listeler `allEntries = true` ile silinir: değişen ürünün hangi `limit` değerine ait listelerde
      olduğu bilinemez.
    - `@CachePut` yerine `@CacheEvict`: `update` cevap dönmüyor; ayrıca evict sonrası ilk okuma
      veriyi her zaman kaynaktan alır.
6. **Sınırlar:** `maximumSize=500` (heap'i korur), `expireAfterWrite=5m` (TTL), `recordStats` (hit/miss metrikleri).
   TTL, evict'in kaçırdığı değişikliklere karşı güvenlik ağıdır (servis dışından DB düzeltmesi, birden fazla instance).
7. Cache'teki nesneler **immutable** olmalıdır (`ProductResponse` record, listeler `.toList()`):
   Caffeine kopya değil referans tutar; nesne değişirse tüm okuyucular etkilenir.

## Değerlendirilen alternatifler

- **Ehcache 3 (JCache):** Kursun tercihi. Ek `ehcache.xml` ve JCache katmanı gerektirir; tek instance'lı
  local cache için Caffeine'e göre kazancı yok. Disk/off-heap depolama gerekirse yeniden değerlendirilir.
- **Redis (distributed cache):** Tüm instance'lar aynı cache'i görür, tutarsızlık sorunu çözülür.
  Bedeli: ayrı altyapı, ağ çağrısı, serileştirme. Birden fazla instance çalıştırıldığında değerlendirilecek.
- **Stoklu okumada da ürün kısmını cache'lemek:** Ürünü okuyan kısmı ayrı bir bean'e taşımayı gerektirir;
  aynı sınıf içinden `this.getById()` çağrısı proxy'den geçmediği için cache devreye girmez (self-invocation).
  Kazanç küçük olduğu için şimdilik uygulanmadı.
- **Kursun yolu (`condition = "#showInventory == false"`):** Task 16a'da `getById` ve `getByIdWithInventory`
  ayrı metotlar olduğu için koşula gerek kalmadı.

## Sonuçlar

**Olumlu**
- JPA geçişinden sonra tekrar eden ürün okumaları veritabanına gitmeyecek.
- Sağlayıcı değişirse (ör. Redis) sadece bağımlılık ve yaml değişir; service kodu değişmez.
- Stok hiçbir zaman eski değil.

**Olumsuz / riskler**
- **Birden fazla instance:** Bir instance'ta yapılan update diğer instance'ların cache'ini temizlemez;
  onlar TTL dolana kadar (en fazla 5 dk) eski veriyi döner. Çözüm seçenekleri: kısa TTL, Redis,
  ya da güncelleme event'i ile tüm instance'larda evict (Kafka, Hafta 3).
- `allEntries = true` her yazmada tüm listeleri siler; yazma sıklığı artarsa liste cache'inin hit oranı düşer.
- Cache anotasyonları proxy'ye bağlıdır: aynı sınıf içinden yapılan çağrılarda çalışmaz.
- Testler: cache davranışı Mockito unit testinde görünmez; `ProductCacheTest` (`@SpringBootTest`) ile doğrulanır.

## İlgili

- ADR-0004: Servis sınırları (stok inventory-service'in verisi)
- Task 16c: `GET /products/sku/{sku}` ve `productsBySku` cache'i — aynı ürünün iki cache'te tutulmasının
  invalidation kararı bu ADR'ye eklenecek.