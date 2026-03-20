import requests
from bs4 import BeautifulSoup
import pandas as pd
import time
import re

def scrape_2026_fast_furious_premium_final_v3():
    base_api_url = "https://hotwheels.fandom.com/api.php"
    page_title = "2026_Fast_&_Furious_Premium_Series"
    all_data = []
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    print(f">>> {page_title} sayfası taranıyor (Numara/Sütun eşleşmesi optimize edildi)...")
    
    params = {
        "action": "parse", "page": page_title, "format": "json", "prop": "text"
    }

    try:
        response = requests.get(base_api_url, params=params, headers=headers)
        data = response.json()
        
        if "error" in data:
            print(f"  ❌ Hata: {data['error'].get('info', 'Sayfa bulunamadı')}")
            return

        soup = BeautifulSoup(data["parse"]["text"]["*"], "html.parser")
        tables = soup.find_all("table", class_=lambda x: x and 'wikitable' in x)
        
        for table in tables:
            series_name = "Fast & Furious Premium"
            prev_h = table.find_previous(['h2', 'h3'])
            if prev_h:
                series_name = prev_h.get_text(strip=True).split('[')[0].strip()

            rows = table.find_all("tr")
            if not rows: continue

            # HASSAS SÜTUN HARİTALAMA
            col_map = {}
            header_cells = rows[0].find_all(["th", "td"])
            for i, cell in enumerate(header_cells):
                txt = cell.get_text(strip=True).lower()
                
                if "casting" in txt or "model" in txt: 
                    col_map["model_name"] = i
                elif "toy" in txt: 
                    col_map["toy_num"] = i
                elif "color" in txt: 
                    col_map["color"] = i
                # 1/5 gibi değerleri yakalamak için 'num' içeren sütunu doğrudan series_num yapıyoruz
                elif "num" in txt or "set #" in txt or "series #" in txt:
                    col_map["series_num"] = i
                # col_num sadece içinde 'col' geçen ama 'color' veya 'num' geçmeyen sütunlara atanır
                elif "col" in txt and "color" not in txt and "num" not in txt:
                    col_map["col_num"] = i
                elif "photo" in txt: 
                    col_map["photo"] = i

            if "model_name" not in col_map: continue

            for row in rows[1:]:
                cells = row.find_all(["td", "th"])
                if len(cells) <= col_map["model_name"]: continue

                def get_val(key):
                    if key in col_map and col_map[key] < len(cells):
                        val = cells[col_map[key]].get_text(strip=True)
                        return val.replace("?", "")
                    return ""

                m_name = get_val("model_name")
                if not m_name or m_name.lower() in ["casting", "model", "tba"]: continue

                img = ""
                if "photo" in col_map:
                    img_tag = cells[col_map["photo"]].find("img")
                    if img_tag:
                        img = (img_tag.get("data-src") or img_tag.get("src", "")).split("/revision/")[0]

                # 1/5 gibi değer bazen col_num sütununa yazılmışsa onu series_num'a kaydır
                s_num = get_val("series_num")
                c_num = get_val("col_num")
                
                if "/" in c_num and not s_num:
                    s_num = c_num
                    c_num = ""

                all_data.append({
                    "year": "2026",
                    "series": series_name,
                    "series_num": s_num,
                    "model_name": m_name,
                    "color": get_val("color"),
                    "toy_num": get_val("toy_num"),
                    "col_num": c_num,
                    "image_url": img,
                    "feature": "",
                    "scale": "1:64",
                    "category": "premium"
                })

        if all_data:
            df = pd.DataFrame(all_data)
            df = df[["year", "series", "series_num", "model_name", "color", "toy_num", "col_num", "image_url", "feature", "scale", "category"]]
            
            output_file = "hotwheels_2026_ff_premium_final.csv"
            df.to_csv(output_file, index=False, encoding="utf-8-sig")
            print(f"\n✅ Düzeltme tamamlandı! '{output_file}' dosyası oluşturuldu.")
        else:
            print("❌ Veri bulunamadı.")

    except Exception as e:
        print(f"❌ Hata: {e}")

if __name__ == "__main__":
    scrape_2026_fast_furious_premium_final_v3()