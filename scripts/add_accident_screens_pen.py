#!/usr/bin/env python3
"""
Adds T11 (Accident Video & Checklist), T12 (New Parts Quote), and T13-Web (Accident Dossier)
to design/mottu-smart-ops.pen.
"""
import json
import uuid

def gen_id():
    return uuid.uuid4().hex[:6]

def create_status_bar():
    return {
        "type": "frame",
        "id": gen_id(),
        "name": "Status Bar",
        "width": "fill_container",
        "height": 44,
        "padding": [0, 20],
        "justifyContent": "space_between",
        "alignItems": "center",
        "children": [
            {
                "type": "text",
                "id": gen_id(),
                "name": "Time",
                "fill": "$ink-primary",
                "content": "12:30",
                "lineHeight": 1.1,
                "fontFamily": "Plus Jakarta Sans",
                "fontSize": 14,
                "fontWeight": "600"
            },
            {
                "type": "frame",
                "id": gen_id(),
                "name": "Icons",
                "gap": 6,
                "alignItems": "center",
                "children": [
                    {"type": "icon", "id": gen_id(), "name": "Signal", "width": 14, "height": 14, "icon": "signal", "library": "lucide", "fill": "$ink-primary"},
                    {"type": "icon", "id": gen_id(), "name": "Wifi", "width": 14, "height": 14, "icon": "wifi", "library": "lucide", "fill": "$ink-primary"},
                    {"type": "icon", "id": gen_id(), "name": "Battery", "width": 20, "height": 12, "icon": "battery-medium", "library": "lucide", "fill": "$ink-primary"}
                ]
            }
        ]
    }

def create_mobile_header(title, show_back=True, badge_text=None):
    left_children = []
    if show_back:
        left_children.append({
            "type": "icon",
            "id": gen_id(),
            "name": "Back Icon",
            "width": 20,
            "height": 20,
            "icon": "arrow-left",
            "library": "lucide",
            "fill": "$ink-primary"
        })
    left_children.append({
        "type": "text",
        "id": gen_id(),
        "name": "Header Title",
        "fill": "$ink-primary",
        "content": title,
        "lineHeight": 1.2,
        "fontFamily": "Plus Jakarta Sans",
        "fontSize": 16,
        "fontWeight": "700"
    })
    
    right_children = []
    if badge_text:
        right_children.append({
            "type": "frame",
            "id": gen_id(),
            "name": "Badge",
            "fill": "#FFB30026",
            "cornerRadius": 6,
            "padding": [4, 8],
            "children": [
                {
                    "type": "text",
                    "id": gen_id(),
                    "name": "Badge Text",
                    "fill": "#FFB300",
                    "content": badge_text,
                    "fontFamily": "JetBrains Mono",
                    "fontSize": 11,
                    "fontWeight": "600"
                }
            ]
        })
    
    return {
        "type": "frame",
        "id": gen_id(),
        "name": "Mobile Header",
        "width": "fill_container",
        "height": 56,
        "fill": "$bg-surface",
        "stroke": "$border-light",
        "strokeWidth": {"bottom": 1},
        "strokeAlignment": "inner",
        "padding": [0, 16],
        "justifyContent": "space_between",
        "alignItems": "center",
        "children": [
            {
                "type": "frame",
                "id": gen_id(),
                "name": "Left Title",
                "gap": 10,
                "alignItems": "center",
                "children": left_children
            },
            {
                "type": "frame",
                "id": gen_id(),
                "name": "Right Actions",
                "gap": 8,
                "alignItems": "center",
                "children": right_children
            }
        ]
    }

def create_t11_accident_record_frame():
    return {
        "type": "frame",
        "id": "T11AccidentFrame",
        "x": 2640,
        "y": 100,
        "name": "T11 - Auto-Relato de Acidente (Vídeo & Checklist)",
        "clip": True,
        "width": 390,
        "height": 844,
        "fill": "$bg-page",
        "cornerRadius": 16,
        "stroke": "$border-base",
        "strokeWidth": 1,
        "strokeAlignment": "inner",
        "layout": "vertical",
        "children": [
            create_status_bar(),
            create_mobile_header("Relatar Acidente / Sinistro", show_back=True, badge_text="SOS 24H"),
            {
                "type": "frame",
                "id": gen_id(),
                "name": "Content Body",
                "width": "fill_container",
                "fill": "$bg-page",
                "layout": "vertical",
                "gap": 12,
                "padding": 16,
                "children": [
                    # Emergency Banner
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Emergency Banner",
                        "width": "fill_container",
                        "fill": "#FF4D4F1A",
                        "cornerRadius": 10,
                        "stroke": "#FF4D4F",
                        "strokeWidth": 1,
                        "padding": 12,
                        "layout": "vertical",
                        "gap": 4,
                        "children": [
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Banner Header",
                                "gap": 8,
                                "alignItems": "center",
                                "children": [
                                    {"type": "icon", "id": gen_id(), "name": "Alert Icon", "width": 16, "height": 16, "icon": "alert-octagon", "library": "lucide", "fill": "#FF4D4F"},
                                    {"type": "text", "id": gen_id(), "name": "Alert Title", "fill": "#FF4D4F", "content": "Houve feridos? Priorize o socorro!", "fontFamily": "Plus Jakarta Sans", "fontSize": 13, "fontWeight": "700"}
                                ]
                            },
                            {
                                "type": "text",
                                "id": gen_id(),
                                "name": "Alert Text",
                                "fill": "$ink-secondary",
                                "content": "Se houver qualquer pessoa ferida, ligue imediatamente para SAMU 192 ou Bombeiros 193.",
                                "fontFamily": "Plus Jakarta Sans",
                                "fontSize": 12,
                                "lineHeight": 1.3
                            }
                        ]
                    },
                    # Video Viewfinder Card
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Video Guide Viewfinder",
                        "width": "fill_container",
                        "height": 200,
                        "fill": "#060A08",
                        "cornerRadius": 12,
                        "stroke": "$border-base",
                        "strokeWidth": 1,
                        "layout": "vertical",
                        "justifyContent": "space_between",
                        "padding": 12,
                        "children": [
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Viewfinder Header",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "children": [
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Rec Pill",
                                        "fill": "#FF4D4F26",
                                        "cornerRadius": 6,
                                        "padding": [3, 8],
                                        "gap": 6,
                                        "alignItems": "center",
                                        "children": [
                                            {"type": "icon", "id": gen_id(), "name": "Dot", "width": 8, "height": 8, "icon": "circle", "library": "lucide", "fill": "#FF4D4F"},
                                            {"type": "text", "id": gen_id(), "name": "Timer", "fill": "#FF4D4F", "content": "00:24 / 00:30", "fontFamily": "JetBrains Mono", "fontSize": 11, "fontWeight": "600"}
                                        ]
                                    },
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Size Badge",
                                        "fill": "#00E6761A",
                                        "cornerRadius": 6,
                                        "padding": [3, 8],
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "Size Text", "fill": "$brand-green", "content": "MP4 15.4 MB (Válido)", "fontFamily": "JetBrains Mono", "fontSize": 11, "fontWeight": "600"}
                                        ]
                                    }
                                ]
                            },
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Center Guide",
                                "layout": "vertical",
                                "alignItems": "center",
                                "gap": 4,
                                "children": [
                                    {"type": "icon", "id": gen_id(), "name": "Cam Icon", "width": 28, "height": 28, "icon": "video", "library": "lucide", "fill": "$brand-green"},
                                    {"type": "text", "id": gen_id(), "name": "Guide Text", "fill": "$ink-primary", "content": "Vídeo gravado: Volta completa 360°", "fontFamily": "Plus Jakarta Sans", "fontSize": 13, "fontWeight": "600"},
                                    {"type": "text", "id": gen_id(), "name": "Subguide", "fill": "$ink-secondary", "content": "1. Frente → 2. Direita → 3. Traseira → 4. Esquerda", "fontFamily": "Plus Jakarta Sans", "fontSize": 11}
                                ]
                            },
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Extra Photos Strip",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "Closes Label", "fill": "$ink-secondary", "content": "+ 1 foto close anexada", "fontFamily": "Plus Jakarta Sans", "fontSize": 11},
                                    {"type": "text", "id": gen_id(), "name": "Retake Link", "fill": "$accent-amber", "content": "Trocar vídeo", "fontFamily": "Plus Jakarta Sans", "fontSize": 12, "fontWeight": "600"}
                                ]
                            }
                        ]
                    },
                    # Checklist Questions Card
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Checklist Card",
                        "width": "fill_container",
                        "fill": "$bg-surface",
                        "cornerRadius": 12,
                        "stroke": "$border-light",
                        "strokeWidth": 1,
                        "padding": 14,
                        "layout": "vertical",
                        "gap": 10,
                        "children": [
                            {
                                "type": "text",
                                "id": gen_id(),
                                "name": "Checklist Title",
                                "fill": "$ink-primary",
                                "content": "Perguntas de Segurança & Frota",
                                "fontFamily": "Plus Jakarta Sans",
                                "fontSize": 13,
                                "fontWeight": "700"
                            },
                            # Third party / B.O.
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Row Third Party",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "L1", "fill": "$ink-secondary", "content": "Envolveu terceiros / B.O.?", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Toggle BO",
                                        "fill": "#1A2320",
                                        "cornerRadius": 6,
                                        "padding": [4, 10],
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "T1", "fill": "$ink-primary", "content": "Não", "fontFamily": "Plus Jakarta Sans", "fontSize": 12, "fontWeight": "600"}
                                        ]
                                    }
                                ]
                            },
                            # Drivability toggle
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Row Drivability",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "L2", "fill": "$ink-secondary", "content": "Condição da moto:", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Badge Guincho",
                                        "fill": "#FFB30026",
                                        "stroke": "#FFB300",
                                        "strokeWidth": 1,
                                        "cornerRadius": 6,
                                        "padding": [4, 10],
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "T2", "fill": "#FFB300", "content": "🚨 Precisa de Guincho (+R$ 80)", "fontFamily": "Plus Jakarta Sans", "fontSize": 11, "fontWeight": "700"}
                                        ]
                                    }
                                ]
                            },
                            # Odometer input
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Row Odometer",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "L3", "fill": "$ink-secondary", "content": "Odômetro no painel:", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Odo Value",
                                        "fill": "#1A2320",
                                        "cornerRadius": 6,
                                        "padding": [4, 10],
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "T3", "fill": "$ink-primary", "content": "1.045 km", "fontFamily": "JetBrains Mono", "fontSize": 12, "fontWeight": "600"}
                                        ]
                                    }
                                ]
                            }
                        ]
                    },
                    # Submit CTA button
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Button Submit Accident",
                        "width": "fill_container",
                        "height": 52,
                        "fill": "$brand-green",
                        "cornerRadius": 8,
                        "gap": 8,
                        "justifyContent": "center",
                        "alignItems": "center",
                        "children": [
                            {"type": "icon", "id": gen_id(), "name": "Check Icon", "width": 18, "height": 18, "icon": "sparkles", "library": "lucide", "fill": "#06120B"},
                            {
                                "type": "text",
                                "id": gen_id(),
                                "name": "Btn Label",
                                "fill": "#06120B",
                                "content": "Analisar Vídeo com IA Mottu",
                                "fontFamily": "Plus Jakarta Sans",
                                "fontSize": 14,
                                "fontWeight": "700"
                            }
                        ]
                    }
                ]
            }
        ]
    }

def create_t12_accident_quote_frame():
    return {
        "type": "frame",
        "id": "T12AccidentQuoteFrame",
        "x": 3080,
        "y": 100,
        "name": "T12 - Orçamento Detalhado de Peças Novas",
        "clip": True,
        "width": 390,
        "height": 844,
        "fill": "$bg-page",
        "cornerRadius": 16,
        "stroke": "$border-base",
        "strokeWidth": 1,
        "strokeAlignment": "inner",
        "layout": "vertical",
        "children": [
            create_status_bar(),
            create_mobile_header("Orçamento de Reposição", show_back=True, badge_text="ESTIMATIVA IA"),
            {
                "type": "frame",
                "id": gen_id(),
                "name": "Content Body",
                "width": "fill_container",
                "fill": "$bg-page",
                "layout": "vertical",
                "gap": 12,
                "padding": 16,
                "children": [
                    # Summary Header Card
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Protocol Header Card",
                        "width": "fill_container",
                        "fill": "$bg-surface",
                        "cornerRadius": 12,
                        "stroke": "$border-light",
                        "strokeWidth": 1,
                        "padding": 14,
                        "layout": "vertical",
                        "gap": 6,
                        "children": [
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Protocol Top",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "Proto Title", "fill": "$ink-primary", "content": "Sinistro #AC-2026-8812", "fontFamily": "JetBrains Mono", "fontSize": 14, "fontWeight": "700"},
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Status Pill",
                                        "fill": "#FFB30026",
                                        "cornerRadius": 6,
                                        "padding": [3, 8],
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "Status Text", "fill": "#FFB300", "content": "Dossiê na Oficina", "fontFamily": "Plus Jakarta Sans", "fontSize": 11, "fontWeight": "700"}
                                        ]
                                    }
                                ]
                            },
                            {"type": "text", "id": gen_id(), "name": "Moto Info", "fill": "$ink-secondary", "content": "Mottu Sport 110i • Placa MOT1A23 • Vídeo 24s", "fontFamily": "Plus Jakarta Sans", "fontSize": 12}
                        ]
                    },
                    # Parts Table Card
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Parts Table Card",
                        "width": "fill_container",
                        "fill": "$bg-surface",
                        "cornerRadius": 12,
                        "stroke": "$border-light",
                        "strokeWidth": 1,
                        "padding": 14,
                        "layout": "vertical",
                        "gap": 8,
                        "children": [
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Table Title Row",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "TH1", "fill": "$ink-primary", "content": "Peças Novas Identificadas (3)", "fontFamily": "Plus Jakarta Sans", "fontSize": 13, "fontWeight": "700"},
                                    {"type": "text", "id": gen_id(), "name": "TH2", "fill": "$ink-secondary", "content": "Preço Oficial", "fontFamily": "Plus Jakarta Sans", "fontSize": 11}
                                ]
                            },
                            # Row 1
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Part Row 1",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "padding": [6, 0],
                                "children": [
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Part Left 1",
                                        "gap": 6,
                                        "alignItems": "center",
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "TS1", "fill": "#29B6F6", "content": "00:06", "fontFamily": "JetBrains Mono", "fontSize": 11, "fontWeight": "600"},
                                            {"type": "text", "id": gen_id(), "name": "PN1", "fill": "$ink-primary", "content": "Manete de Freio Direito", "fontFamily": "Plus Jakarta Sans", "fontSize": 12}
                                        ]
                                    },
                                    {"type": "text", "id": gen_id(), "name": "PR1", "fill": "$ink-primary", "content": "R$ 25,00", "fontFamily": "JetBrains Mono", "fontSize": 12, "fontWeight": "600"}
                                ]
                            },
                            # Row 2
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Part Row 2",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "padding": [6, 0],
                                "children": [
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Part Left 2",
                                        "gap": 6,
                                        "alignItems": "center",
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "TS2", "fill": "#29B6F6", "content": "00:14", "fontFamily": "JetBrains Mono", "fontSize": 11, "fontWeight": "600"},
                                            {"type": "text", "id": gen_id(), "name": "PN2", "fill": "$ink-primary", "content": "Retrovisor Direito", "fontFamily": "Plus Jakarta Sans", "fontSize": 12}
                                        ]
                                    },
                                    {"type": "text", "id": gen_id(), "name": "PR2", "fill": "$ink-primary", "content": "R$ 42,50", "fontFamily": "JetBrains Mono", "fontSize": 12, "fontWeight": "600"}
                                ]
                            },
                            # Row 3
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Part Row 3",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "padding": [6, 0],
                                "children": [
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Part Left 3",
                                        "gap": 6,
                                        "alignItems": "center",
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "TS3", "fill": "#29B6F6", "content": "00:21", "fontFamily": "JetBrains Mono", "fontSize": 11, "fontWeight": "600"},
                                            {"type": "text", "id": gen_id(), "name": "PN3", "fill": "$ink-primary", "content": "Pedal de Apoio Dianteiro", "fontFamily": "Plus Jakarta Sans", "fontSize": 12}
                                        ]
                                    },
                                    {"type": "text", "id": gen_id(), "name": "PR3", "fill": "$ink-primary", "content": "R$ 80,00", "fontFamily": "JetBrains Mono", "fontSize": 12, "fontWeight": "600"}
                                ]
                            }
                        ]
                    },
                    # Calculation & Breakdown Card
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Calculation Card",
                        "width": "fill_container",
                        "fill": "$bg-surface",
                        "cornerRadius": 12,
                        "stroke": "$border-light",
                        "strokeWidth": 1,
                        "padding": 14,
                        "layout": "vertical",
                        "gap": 8,
                        "children": [
                            {"type": "text", "id": gen_id(), "name": "Calc Title", "fill": "$ink-primary", "content": "Composição do Total", "fontFamily": "Plus Jakarta Sans", "fontSize": 13, "fontWeight": "700"},
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "R1 Subtotal",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "CL1", "fill": "$ink-secondary", "content": "Subtotal Peças Novas:", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                    {"type": "text", "id": gen_id(), "name": "CV1", "fill": "$ink-primary", "content": "R$ 147,50", "fontFamily": "JetBrains Mono", "fontSize": 12}
                                ]
                            },
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "R2 Labor",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "CL2", "fill": "$ink-secondary", "content": "Mão de Obra Oficina (3+ peças):", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                    {"type": "text", "id": gen_id(), "name": "CV2", "fill": "$ink-primary", "content": "R$ 90,00", "fontFamily": "JetBrains Mono", "fontSize": 12}
                                ]
                            },
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "R3 Towing",
                                "width": "fill_container",
                                "justifyContent": "space_between",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "CL3", "fill": "$ink-secondary", "content": "Taxa de Guincho / Recolhimento:", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                    {"type": "text", "id": gen_id(), "name": "CV3", "fill": "#FFB300", "content": "R$ 80,00", "fontFamily": "JetBrains Mono", "fontSize": 12, "fontWeight": "600"}
                                ]
                            },
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Total Highlight",
                                "width": "fill_container",
                                "fill": "#1A2320",
                                "cornerRadius": 8,
                                "padding": 12,
                                "justifyContent": "space_between",
                                "alignItems": "center",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "TL", "fill": "$ink-primary", "content": "Total Estimado:", "fontFamily": "Plus Jakarta Sans", "fontSize": 14, "fontWeight": "700"},
                                    {"type": "text", "id": gen_id(), "name": "TV", "fill": "$brand-green", "content": "R$ 317,50", "fontFamily": "JetBrains Mono", "fontSize": 18, "fontWeight": "700"}
                                ]
                            }
                        ]
                    },
                    # Note to mechanic input
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Note Box Card",
                        "width": "fill_container",
                        "fill": "$bg-surface",
                        "cornerRadius": 10,
                        "stroke": "$border-light",
                        "strokeWidth": 1,
                        "padding": 10,
                        "layout": "vertical",
                        "gap": 4,
                        "children": [
                            {"type": "text", "id": gen_id(), "name": "Note Title", "fill": "$ink-secondary", "content": "Observação para o Mecânico (Opcional):", "fontFamily": "Plus Jakarta Sans", "fontSize": 11},
                            {"type": "text", "id": gen_id(), "name": "Note Content", "fill": "$ink-primary", "content": "A carenagem só ralou um pouco, o manete quebrou na ponta.", "fontFamily": "Plus Jakarta Sans", "fontSize": 12}
                        ]
                    },
                    # Confirm CTA Button
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Button Confirm Quote",
                        "width": "fill_container",
                        "height": 52,
                        "fill": "$brand-green",
                        "cornerRadius": 8,
                        "justifyContent": "center",
                        "alignItems": "center",
                        "children": [
                            {"type": "text", "id": gen_id(), "name": "Btn Text", "fill": "#06120B", "content": "Confirmar Protocolo e Solicitar Guincho", "fontFamily": "Plus Jakarta Sans", "fontSize": 14, "fontWeight": "700"}
                        ]
                    }
                ]
            }
        ]
    }

def create_t13_accident_dossier_web_frame():
    return {
        "type": "frame",
        "id": "T13AccidentDossierWebFrame",
        "x": 3000,
        "y": 2180,
        "name": "T13-Web - Dossiê de Acidente na Oficina (Desktop)",
        "clip": True,
        "width": 1440,
        "height": 900,
        "fill": "$bg-page",
        "cornerRadius": 8,
        "stroke": "$border-base",
        "strokeWidth": 1,
        "strokeAlignment": "inner",
        "children": [
            # Sidebar
            {
                "type": "frame",
                "id": gen_id(),
                "name": "Sidebar",
                "width": 240,
                "height": 900,
                "fill": "$bg-surface",
                "stroke": "$border-light",
                "strokeWidth": {"right": 1},
                "strokeAlignment": "inner",
                "layout": "vertical",
                "padding": 20,
                "gap": 24,
                "children": [
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Brand",
                        "gap": 8,
                        "alignItems": "center",
                        "children": [
                            {"type": "icon", "id": gen_id(), "name": "Brand Icon", "width": 22, "height": 22, "icon": "wrench", "library": "lucide", "fill": "$brand-green"},
                            {"type": "text", "id": gen_id(), "name": "Brand Name", "fill": "$ink-primary", "content": "Mottu Oficina", "fontFamily": "Plus Jakarta Sans", "fontSize": 16, "fontWeight": "700"}
                        ]
                    },
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Nav Items",
                        "layout": "vertical",
                        "gap": 6,
                        "children": [
                            {"type": "text", "id": gen_id(), "name": "N1", "fill": "$ink-secondary", "content": "Fila da Oficina", "fontFamily": "Plus Jakarta Sans", "fontSize": 13},
                            {"type": "text", "id": gen_id(), "name": "N2", "fill": "$ink-secondary", "content": "Vistorias Regulares", "fontFamily": "Plus Jakarta Sans", "fontSize": 13},
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Nav Active",
                                "fill": "#00E6761A",
                                "cornerRadius": 6,
                                "padding": [8, 12],
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "N3", "fill": "$brand-green", "content": "🚨 Dossiês de Sinistro (1)", "fontFamily": "Plus Jakarta Sans", "fontSize": 13, "fontWeight": "700"}
                                ]
                            },
                            {"type": "text", "id": gen_id(), "name": "N4", "fill": "$ink-secondary", "content": "Frota do Galpão", "fontFamily": "Plus Jakarta Sans", "fontSize": 13}
                        ]
                    }
                ]
            },
            # Main Content Area
            {
                "type": "frame",
                "id": gen_id(),
                "x": 240,
                "y": 0,
                "name": "Main",
                "width": 1200,
                "height": 900,
                "layout": "vertical",
                "children": [
                    # Top Header
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Top Header",
                        "width": 1200,
                        "height": 64,
                        "fill": "$bg-surface",
                        "stroke": "$border-light",
                        "strokeWidth": {"bottom": 1},
                        "padding": [0, 24],
                        "justifyContent": "space_between",
                        "alignItems": "center",
                        "children": [
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Header Left",
                                "gap": 12,
                                "alignItems": "center",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "Dossier Title", "fill": "$ink-primary", "content": "Dossiê de Acidente #AC-2026-8812", "fontFamily": "Plus Jakarta Sans", "fontSize": 16, "fontWeight": "700"},
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Plate Badge",
                                        "fill": "#1A2320",
                                        "cornerRadius": 6,
                                        "padding": [4, 8],
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "Plate Text", "fill": "$brand-green", "content": "MOT1A23 • Mottu Sport 110i", "fontFamily": "JetBrains Mono", "fontSize": 12, "fontWeight": "600"}
                                        ]
                                    },
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Towing Badge",
                                        "fill": "#FFB30026",
                                        "cornerRadius": 6,
                                        "padding": [4, 8],
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "Towing Text", "fill": "#FFB300", "content": "🚨 GUINCHO SOLICITADO", "fontFamily": "Plus Jakarta Sans", "fontSize": 11, "fontWeight": "700"}
                                        ]
                                    }
                                ]
                            },
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Header Right",
                                "gap": 10,
                                "alignItems": "center",
                                "children": [
                                    {"type": "text", "id": gen_id(), "name": "Status Label", "fill": "$ink-secondary", "content": "Status:", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                    {"type": "text", "id": gen_id(), "name": "Status Val", "fill": "#FFB300", "content": "ESTIMATED_PENDING_WORKSHOP", "fontFamily": "JetBrains Mono", "fontSize": 12, "fontWeight": "600"}
                                ]
                            }
                        ]
                    },
                    # Workspace 55% / 45%
                    {
                        "type": "frame",
                        "id": gen_id(),
                        "name": "Workspace",
                        "width": 1200,
                        "height": 836,
                        "padding": 20,
                        "gap": 20,
                        "children": [
                            # Left Column (55% -> 630px)
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Left Column Video & Context",
                                "width": 630,
                                "height": 796,
                                "layout": "vertical",
                                "gap": 16,
                                "children": [
                                    # Video Player Box
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Video Player Card",
                                        "width": "fill_container",
                                        "height": 380,
                                        "fill": "#060A08",
                                        "cornerRadius": 12,
                                        "stroke": "$border-base",
                                        "strokeWidth": 1,
                                        "padding": 16,
                                        "layout": "vertical",
                                        "justifyContent": "space_between",
                                        "children": [
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Player Header",
                                                "width": "fill_container",
                                                "justifyContent": "space_between",
                                                "children": [
                                                    {"type": "text", "id": gen_id(), "name": "Vid Title", "fill": "$ink-primary", "content": "Vídeo Gravado na Rua (24s)", "fontFamily": "Plus Jakarta Sans", "fontSize": 13, "fontWeight": "600"},
                                                    {"type": "text", "id": gen_id(), "name": "Vid Info", "fill": "$ink-secondary", "content": "Resolução 1080p • 15.4 MB", "fontFamily": "JetBrains Mono", "fontSize": 11}
                                                ]
                                            },
                                            # Viewport simulation
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Playback Area",
                                                "layout": "vertical",
                                                "alignItems": "center",
                                                "gap": 8,
                                                "children": [
                                                    {"type": "icon", "id": gen_id(), "name": "Play Icon", "width": 48, "height": 48, "icon": "play-circle", "library": "lucide", "fill": "$brand-green"},
                                                    {"type": "text", "id": gen_id(), "name": "Play Text", "fill": "$ink-primary", "content": "Clique para reproduzir com marcas de avaria", "fontFamily": "Plus Jakarta Sans", "fontSize": 13}
                                                ]
                                            },
                                            # Timeline with markers
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Timeline Box",
                                                "width": "fill_container",
                                                "layout": "vertical",
                                                "gap": 6,
                                                "children": [
                                                    {
                                                        "type": "frame",
                                                        "id": gen_id(),
                                                        "name": "Marker Buttons Strip",
                                                        "gap": 8,
                                                        "children": [
                                                            {
                                                                "type": "frame",
                                                                "id": gen_id(),
                                                                "name": "P1 Marker",
                                                                "fill": "#29B6F626",
                                                                "cornerRadius": 6,
                                                                "padding": [4, 8],
                                                                "children": [
                                                                    {"type": "text", "id": gen_id(), "name": "T1", "fill": "#29B6F6", "content": "⏱ 00:06 (Manete)", "fontFamily": "JetBrains Mono", "fontSize": 11, "fontWeight": "600"}
                                                                ]
                                                            },
                                                            {
                                                                "type": "frame",
                                                                "id": gen_id(),
                                                                "name": "P2 Marker",
                                                                "fill": "#29B6F626",
                                                                "cornerRadius": 6,
                                                                "padding": [4, 8],
                                                                "children": [
                                                                    {"type": "text", "id": gen_id(), "name": "T2", "fill": "#29B6F6", "content": "⏱ 00:14 (Retrovisor)", "fontFamily": "JetBrains Mono", "fontSize": 11, "fontWeight": "600"}
                                                                ]
                                                            },
                                                            {
                                                                "type": "frame",
                                                                "id": gen_id(),
                                                                "name": "P3 Marker",
                                                                "fill": "#29B6F626",
                                                                "cornerRadius": 6,
                                                                "padding": [4, 8],
                                                                "children": [
                                                                    {"type": "text", "id": gen_id(), "name": "T3", "fill": "#29B6F6", "content": "⏱ 00:21 (Pedal)", "fontFamily": "JetBrains Mono", "fontSize": 11, "fontWeight": "600"}
                                                                ]
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    },
                                    # Checklist & Telemetry Card
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Context Dossier Card",
                                        "width": "fill_container",
                                        "fill": "$bg-surface",
                                        "cornerRadius": 12,
                                        "stroke": "$border-light",
                                        "strokeWidth": 1,
                                        "padding": 16,
                                        "layout": "vertical",
                                        "gap": 10,
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "Ctx Title", "fill": "$ink-primary", "content": "Relato do Entregador & Telemetria", "fontFamily": "Plus Jakarta Sans", "fontSize": 13, "fontWeight": "700"},
                                            {"type": "text", "id": gen_id(), "name": "C1", "fill": "$ink-secondary", "content": "• Entregador: Carlos da Silva (CPF: 123.456.***-01)", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                            {"type": "text", "id": gen_id(), "name": "C2", "fill": "$ink-secondary", "content": "• Vítimas/Feridos: Não | Terceiros/BO: Não registrado", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                            {"type": "text", "id": gen_id(), "name": "C3", "fill": "$ink-secondary", "content": "• Odômetro no momento da queda: 1.045 km (45 km rodados)", "fontFamily": "Plus Jakarta Sans", "fontSize": 12},
                                            {"type": "text", "id": gen_id(), "name": "C4", "fill": "$ink-primary", "content": "• Observação do Entregador: 'A carenagem só ralou um pouco, o manete quebrou na ponta.'", "fontFamily": "Plus Jakarta Sans", "fontSize": 12}
                                        ]
                                    }
                                ]
                            },
                            # Right Column (45% -> 510px)
                            {
                                "type": "frame",
                                "id": gen_id(),
                                "name": "Right Column Inspection & Resolve",
                                "width": 510,
                                "height": 796,
                                "fill": "$bg-surface",
                                "cornerRadius": 12,
                                "stroke": "$border-light",
                                "strokeWidth": 1,
                                "padding": 16,
                                "layout": "vertical",
                                "justifyContent": "space_between",
                                "children": [
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Checklist Content",
                                        "width": "fill_container",
                                        "layout": "vertical",
                                        "gap": 12,
                                        "children": [
                                            {"type": "text", "id": gen_id(), "name": "Conf Title", "fill": "$ink-primary", "content": "Conferência Física das Peças", "fontFamily": "Plus Jakarta Sans", "fontSize": 14, "fontWeight": "700"},
                                            # Item 1
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Item Row 1",
                                                "width": "fill_container",
                                                "justifyContent": "space_between",
                                                "alignItems": "center",
                                                "padding": [6, 0],
                                                "children": [
                                                    {"type": "text", "id": gen_id(), "name": "It1", "fill": "$ink-primary", "content": "☑ Manete de Freio Direito (R$ 25,00)", "fontFamily": "Plus Jakarta Sans", "fontSize": 13},
                                                    {"type": "text", "id": gen_id(), "name": "St1", "fill": "$brand-green", "content": "Confirmado", "fontFamily": "Plus Jakarta Sans", "fontSize": 11, "fontWeight": "600"}
                                                ]
                                            },
                                            # Item 2
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Item Row 2",
                                                "width": "fill_container",
                                                "justifyContent": "space_between",
                                                "alignItems": "center",
                                                "padding": [6, 0],
                                                "children": [
                                                    {"type": "text", "id": gen_id(), "name": "It2", "fill": "$ink-primary", "content": "☑ Retrovisor Direito (R$ 42,50)", "fontFamily": "Plus Jakarta Sans", "fontSize": 13},
                                                    {"type": "text", "id": gen_id(), "name": "St2", "fill": "$brand-green", "content": "Confirmado", "fontFamily": "Plus Jakarta Sans", "fontSize": 11, "fontWeight": "600"}
                                                ]
                                            },
                                            # Item 3
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Item Row 3",
                                                "width": "fill_container",
                                                "justifyContent": "space_between",
                                                "alignItems": "center",
                                                "padding": [6, 0],
                                                "children": [
                                                    {"type": "text", "id": gen_id(), "name": "It3", "fill": "$ink-primary", "content": "☑ Pedal de Apoio Dianteiro (R$ 80,00)", "fontFamily": "Plus Jakarta Sans", "fontSize": 13},
                                                    {"type": "text", "id": gen_id(), "name": "St3", "fill": "$brand-green", "content": "Confirmado", "fontFamily": "Plus Jakarta Sans", "fontSize": 11, "fontWeight": "600"}
                                                ]
                                            },
                                            # Add extra part button
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Btn Add Part",
                                                "width": "fill_container",
                                                "height": 38,
                                                "fill": "#1A2320",
                                                "stroke": "$border-light",
                                                "strokeWidth": 1,
                                                "cornerRadius": 6,
                                                "justifyContent": "center",
                                                "alignItems": "center",
                                                "children": [
                                                    {"type": "text", "id": gen_id(), "name": "Add Text", "fill": "$ink-primary", "content": "+ Adicionar outra peça danificada", "fontFamily": "Plus Jakarta Sans", "fontSize": 12, "fontWeight": "600"}
                                                ]
                                            },
                                            # Financial Summary Box
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Financial Box",
                                                "width": "fill_container",
                                                "fill": "#0A0E0D",
                                                "cornerRadius": 8,
                                                "padding": 12,
                                                "layout": "vertical",
                                                "gap": 6,
                                                "children": [
                                                    {"type": "text", "id": gen_id(), "name": "Subtotal P", "fill": "$ink-secondary", "content": "Peças: R$ 147,50 • Mão de Obra: R$ 90,00 • Guincho: R$ 80,00", "fontFamily": "Plus Jakarta Sans", "fontSize": 11},
                                                    {"type": "text", "id": gen_id(), "name": "Total Value", "fill": "$brand-green", "content": "Total Confirmado: R$ 317,50", "fontFamily": "JetBrains Mono", "fontSize": 16, "fontWeight": "700"}
                                                ]
                                            }
                                        ]
                                    },
                                    # Mechanic Notes and Approval CTA
                                    {
                                        "type": "frame",
                                        "id": gen_id(),
                                        "name": "Mechanic Actions Bottom",
                                        "width": "fill_container",
                                        "layout": "vertical",
                                        "gap": 10,
                                        "children": [
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Notes Input Box",
                                                "width": "fill_container",
                                                "height": 50,
                                                "fill": "#121816",
                                                "stroke": "$border-light",
                                                "strokeWidth": 1,
                                                "cornerRadius": 6,
                                                "padding": 8,
                                                "children": [
                                                    {"type": "text", "id": gen_id(), "name": "Note Input Text", "fill": "$ink-secondary", "content": "Parecer do mecânico: Peças conferidas na bancada...", "fontFamily": "Plus Jakarta Sans", "fontSize": 12}
                                                ]
                                            },
                                            {
                                                "type": "frame",
                                                "id": gen_id(),
                                                "name": "Button Confirm Dossier",
                                                "width": "fill_container",
                                                "height": 52,
                                                "fill": "$brand-green",
                                                "cornerRadius": 8,
                                                "justifyContent": "center",
                                                "alignItems": "center",
                                                "children": [
                                                    {"type": "text", "id": gen_id(), "name": "Btn Approve", "fill": "#06120B", "content": "Aprovar Laudo Físico (WORKSHOP_CONFIRMED)", "fontFamily": "Plus Jakarta Sans", "fontSize": 14, "fontWeight": "700"}
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        ]
    }

def main():
    pen_path = 'design/mottu-smart-ops.pen'
    with open(pen_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # 1. Update T02 with accident CTA if not present
    t02 = data['children'][2]
    hero = t02['children'][2]['children'][0]
    btn_names = [c.get('name') for c in hero['children']]
    if 'Button Relatar Acidente' not in btn_names:
        hero['children'].append({
            'type': 'frame',
            'id': 'btnAccidentT02',
            'name': 'Button Relatar Acidente',
            'width': 'fill_container',
            'height': 48,
            'fill': '#1A2320',
            'stroke': '#FFB300',
            'strokeWidth': 1.5,
            'strokeAlignment': 'inner',
            'cornerRadius': 8,
            'gap': 8,
            'padding': [0, 16],
            'justifyContent': 'center',
            'alignItems': 'center',
            'children': [
                {
                    'type': 'icon',
                    'id': 'iconAccidentT02',
                    'name': 'Btn Icon Alert',
                    'width': 17,
                    'height': 17,
                    'icon': 'alert-triangle',
                    'library': 'lucide',
                    'fill': '#FFB300'
                },
                {
                    'type': 'text',
                    'id': 'txtAccidentT02',
                    'name': 'Btn Label',
                    'fill': '#FFB300',
                    'content': '🚨 Relatar Acidente / Sinistro',
                    'lineHeight': 1.1,
                    'fontFamily': 'Plus Jakarta Sans',
                    'fontSize': 14,
                    'fontWeight': '600'
                }
            ]
        })
        print('Added accident button to T02 hero!')

    # Remove previous T11AccidentFrame, T12AccidentQuoteFrame, T13AccidentDossierWebFrame if re-running
    data['children'] = [c for c in data['children'] if c.get('id') not in ('T11AccidentFrame', 'T12AccidentQuoteFrame', 'T13AccidentDossierWebFrame')]

    # Add T11, T12, T13-Web
    t11 = create_t11_accident_record_frame()
    t12 = create_t12_accident_quote_frame()
    t13_web = create_t13_accident_dossier_web_frame()

    data['children'].append(t11)
    data['children'].append(t12)
    data['children'].append(t13_web)

    # Expand top section width so all mobile screens fit cleanly
    data['children'][0]['width'] = 3600

    with open(pen_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    count = len(data["children"])
    print(f'Successfully refreshed {pen_path} with T11, T12, and T13-Web! Total top-level elements: {count}')

if __name__ == '__main__':
    main()
