import requests
from bs4 import BeautifulSoup
import json
import time
import re

def scrape_2026_th_sth_json():
    # 1. all_data listesini en başta tanımlıyoruz (Hata almamak için)
    all_data = [] 
    base_api_url = "https://hotwheels.fandom.com/api.php"
    page_title = "2026_Treasure_Hunts_Series"
    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}

    print(f">>> {page_title} sayfası taranıyor (JSON Formatı)...")
    
    params = {
        "action": "parse", 
        "page": page_title, 
        "format": "json", 
        "prop": "text"
    }

    try:
        response = requests.get(base_api_url, params=params, headers=headers, timeout=30)
        data = response.json()
        
        if "error" in data:
            print(f"  ❌ Hata: {data['error'].get('info', 'Sayfa bulunamadı')}")
            return

        soup = BeautifulSoup(data["parse"]["text"]["*"], "html.parser")
        tables = soup.find_all("table", class_=lambda x: x and 'wikitable' in x)
        
        for table in tables:
            # --- TH / STH AYRIMI ---
            feature_val = "th"
            prev_h = table.find_previous(['h2', 'h3'])
            if prev_h:
                h_text = prev_h.get_text(strip=True).lower()
                # 2026 yılı 2011'den büyük olduğu için başlıkta 'super' arıyoruz
                if "super" in h_text or "sth" in h_text:
                    feature_val = "sth"
                else:
                    feature_val = "th"

            rows = table.find_all("tr")
            if not rows: continue

            # Sütun Haritalama
            col_map = {}
            header_cells = rows[0].find_all(["th", "td"])
            for i, cell in enumerate(header_cells):
                txt = cell.get_text(strip=True).lower()
                if "model" in txt or "casting" in txt: col_map["model_name"] = i
                elif "toy" in txt: col_map["toy_num"] = i
                elif "series" in txt: col_map["series"] = i
                elif "num" in txt or "set #" in txt: col_map["series_num"] = i
                elif "color" in txt: col_map["color"] = i
                elif "photo" in txt: col_map["photo"] = i
                elif any(k in txt for k in ["case", "wave", "mix"]): col_map["case_num"] = i

            for row in rows[1:]:
                cells = row.find_all(["td", "th"])
                if len(cells) <= col_map.get("model_name", -1): continue

                def get_val(key):
                    if key in col_map and col_map[key] < len(cells):
                        return cells[col_map[key]].get_text(strip=True).replace("?", "")
                    return ""

                m_name = get_val("model_name")
                if not m_name or m_name.lower() in ["casting", "model", "tba"]: continue

                # --- BİTİŞİK SERİ AYRIŞTIRMA (Compact Kings 9/10 vb.) ---
                raw_series = get_val("series")
                series_name = raw_series
                series_num = get_val("series_num")

                # "Metin 9/10" yapısını regex ile bölüp yerlerine dağıtıyoruz
                match = re.search(r'(.*?)(\d+/\d+)', raw_series)
                if match:
                    series_name = match.group(1).strip()
                    series_num = match.group(2).strip()
                
                # Seri adı boşsa varsayılan Treasure Hunts yap
                if not series_name:
                    series_name = "Treasure Hunts"

                # Resim URL Temizliği
                img = ""
                if "photo" in col_map:
                    img_tag = cells[col_map["photo"]].find("img")
                    if img_tag:
                        img = (img_tag.get("data-src") or img_tag.get("src", "")).split("/revision/")[0]

                # Veri toplama
                all_data.append({
                    "year": "2026",
                    "model_name": m_name,
                    "series": series_name,
                    "series_num": series_num,
                    "color": get_val("color"),
                    "toy_num": get_val("toy_num"),
                    "case_num": get_val("case_num"),
                    "image_url": img,
                    "feature": feature_val,
                    "category": "mainline",
                    "scale": "1:64",
                    "data_source": "Fandom_TH_2026"
                })

    except Exception as e:
        print(f"  ❌ Hata: {e}")

    # 2. JSON Dosyasına Kaydetme
    if all_data:
        output_file = "hotwheels_2026_th_sth.json"
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(all_data, f, ensure_ascii=False, indent=4)
        print(f"\n✅ Başarılı! {len(all_data)} araç çekildi.")
        print(f"📁 Dosya oluşturuldu: {output_file}")
    else:
        print("❌ Veri toplanamadı.")

if __name__ == "__main__":
    scrape_2026_th_sth_json()