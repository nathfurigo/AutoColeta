import time
import pyperclip
from pywinauto.application import Application
from pywinauto.keyboard import send_keys # Necessário para o Ctrl+C

# --- DADOS PARA A PESQUISA ---
# Agora vamos usar o DTM como chave de pesquisa
dados_dtms = [
    {"DTM": "200089231", "id_PedidoColeta": "750512"},
    {"DTM": "200089832", "id_PedidoColeta": "750236"},
    {"DTM": "200090043", "id_PedidoColeta": "749989"},
    {"DTM": "200091842", "id_PedidoColeta": "753377"},
    {"DTM": "200092720", "id_PedidoColeta": "753369"},
    {"DTM": "200093446", "id_PedidoColeta": "753521"},
    {"DTM": "200094051", "id_PedidoColeta": "754898"},
    {"DTM": "200094712", "id_PedidoColeta": "755430"},
    {"DTM": "200097552", "id_PedidoColeta": "757838"},
    {"DTM": "200098376", "id_PedidoColeta": "758742"},
    # ... Adicione o resto das suas DTMs aqui ...
]

# Lista para guardar os resultados
resultados_finais = []

try:
    # --- 1. Conectar ao App ---
    titulo_app = "(SISLOG) Sistema de informações logísticas (Aéreo)"
    print(f"Conectando ao aplicativo: '{titulo_app}'...")
    app = Application(backend="uia").connect(title=titulo_app)
    
    # Pega a janela principal
    main_window = app.window(title=titulo_app)
    main_window.set_focus()
    print("Conectado. Focando na janela.")
    time.sleep(1)

    # --- 2. Navegação até o SAC ---
    print("Navegando: Comercial -> S.A.C....")
    main_window.menu_select("Comercial->S.A.C.")
    
    print("Janela SAC aberta.")
    time.sleep(3) # Espera a janela do SAC carregar

    # --- 3. Conectar à janela do SAC ---
    print("Conectando à janela do SAC...")
    sac_window = main_window.child_window(title="SAC - Atendimento ao cliente", control_type="Window")
    sac_window.wait('ready', timeout=20)
    print("Conectado ao SAC.")
    
    # --- 4. Loop de Pesquisa (VERSÃO CORRIGIDA v6) ---
    
    # Identifica os controles UMA VEZ antes do loop
    print("Identificando campos e botões...")
    
    # PONTO DE CORREÇÃO: Mirar no campo "Ped.Cliente" (auto_id="12")
    campo_ped_cliente = sac_window.child_window(auto_id="12", control_type="Edit")
    
    btn_pesquisar = sac_window.child_window(title="Pesquisar", auto_id="20", control_type="Button")
    print("Campos identificados. Iniciando o loop...")

    for item in dados_dtms:
        dtm_num = item['DTM'] # Esta é a chave de pesquisa agora
        
        print(f"\nProcessando DTM (no campo Ped.Cliente): {dtm_num}")
        
        try:
            # A. Limpa o campo (como solicitado: "exclua o anterior")
            campo_ped_cliente.set_text("")
            
            # B. Digita o novo ID. .set_text() é mais confiável que send_keys
            campo_ped_cliente.set_text(dtm_num)
            time.sleep(0.5)
            
            # C. Clica em Pesquisar
            btn_pesquisar.click()
            
            # D. Espera a pesquisa
            time.sleep(2) 
            
            # E. Limpa o clipboard
            pyperclip.copy("") 
            
            # F. Clica na célula da grade
            # (X=190, Y=300) é o chute para a 2ª coluna, 1ª linha ("Minuta/ CT-e")
            # AJUSTE (X, Y) SE O CLIQUE FOR NO LUGAR ERRADO
            coords_clique_grid = (190, 300) 
            sac_window.click_input(coords=coords_clique_grid)
            time.sleep(0.5)
            
            # G. Simula "Ctrl + C"
            send_keys('^c') # send_keys global é robusto para isso
            time.sleep(0.5)
            
            # H. Lê o clipboard e limpa espaços extras
            minuta_cte = pyperclip.paste().strip()
            
            # I. Validar o resultado
            if minuta_cte: # Checa se a string NÃO é vazia
                print(f"  -> Sucesso! Minuta/CT-e encontrada: {minuta_cte}")
                resultado_minuta = minuta_cte
            else:
                print("  -> Pesquisa OK, mas não consegui copiar o valor da grade (verifique as coordenadas).")
                resultado_minuta = "ERRO_LEITURA_GRID"
                
            resultados_finais.append({
                "DTM": dtm_num, 
                "Minuta_CTE": resultado_minuta
            })

        except Exception as e_loop:
            print(f"  -> Erro ao processar DTM {dtm_num}: {e_loop}")
            resultados_finais.append({"DTM": dtm_num, "Minuta_CTE": "ERRO_SCRIPT"})
            
            # Tenta limpar o campo para o próximo loop não falhar
            try:
                campo_ped_cliente.set_text("")
            except:
                pass 

except Exception as e_main:
    print(f"\n--- ERRO FATAL NA AUTOMAÇÃO ---")
    print(f"Detalhe: {e_main}")

finally:
    print("\n--- PROCESSAMENTO CONCLUÍDO ---")
    for res in resultados_finais:
        print(res)