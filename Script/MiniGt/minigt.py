import requests
from bs4 import BeautifulSoup
import json
import time

def scrape_mini_gt_full_collection():
    all_cars = []
    # Sadece b_id=13 (Full Collection) üzerinden tarama yapıyoruz
    base_url = "https://minigt.tsm-models.com/index.php?action=product-list&b_id=13"
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
    }

    current_page = 1
    print("MiniGT Full Collection taraması başlatılıyor...")

    while True:
        # Sayfa numarası p parametresi ile ekleniyor
        url = f"{base_url}&p={current_page}"
        try:
            response = requests.get(url, headers=headers, timeout=15)
            if response.status_code != 200:
                print(f"Hata: Sayfa {current_page} yüklenemedi.")
                break
            
            soup = BeautifulSoup(response.content, 'html.parser')
            
            # Ürünleri Bulma
            products = soup.find_all('div', class_='pd-list-in')
            
            if not products:
                print(f"Sayfa {current_page}'de ürün bulunamadı. Tarama tamamlanmış olabilir.")
                break

            print(f"  Sayfa {current_page} taranıyor... ({len(products)} araç bulundu)")

            for product in products:
                name_tag = product.find('a', class_='font-weight-bold')
                sku_tag = product.find('p', class_='m-0')
                status_tag = product.find('div', style=lambda v: v and 'font-size: 14px' in v)
                
                # Fotoğraf Linkini Çekme
                img_tag = product.find('img')
                img_url = ""
                if img_tag and img_tag.get('src'):
                    img_url = "https://minigt.tsm-models.com/" + img_tag.get('src')

                # Kategori/Seri bilgisi bu sayfada genellikle ürün adının içinde veya 
                # breadcrumb kısmında olur. Full Collection sayfasında "Full Collection" yazar.
                car_data = {
                    "brand": "MiniGT",
                    "series": "Full Collection",
                    "model_name": name_tag.text.strip() if name_tag else "N/A",
                    "toy_num": sku_tag.text.strip() if sku_tag else "N/A",
                    "status": status_tag.text.strip() if status_tag else "N/A",
                    "image_url": img_url,
                    "detail_url": "https://minigt.tsm-models.com/" + name_tag['href'] if name_tag else "",
                    "scale": "1/64"
                }
                all_cars.append(car_data)

            # Sayfalama Kontrolü (Pagination)
            # Sayfa numarasını içeren 'cdp' div'i içindeki 'next' butonuna bakıyoruz
            pagination = soup.find('div', class_='cdp')
            has_next = False
            if pagination:
                # 'next' yazısını içeren linki ara
                next_link = pagination.find('a', string=lambda s: s and 'next' in s.lower())
                if next_link:
                    has_next = True
            
            if has_next:
                current_page += 1
                time.sleep(0.5) # Siteyi yormamak için kısa bekleme
            else:
                print("Son sayfaya ulaşıldı.")
                break 

        except Exception as e:
            print(f"Hata oluştu (Sayfa: {current_page}): {e}")
            break

    # Tüm veriyi kaydet
    filename = 'minigt_full_collection.json'
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(all_cars, f, ensure_ascii=False, indent=4)
    
    print(f"\nİşlem TAMAMLANDI. Toplam {len(all_cars)} araç '{filename}' dosyasına kaydedildi.")

# Scripti çalıştır
if __name__ == "__main__":
    scrape_mini_gt_full_collection()