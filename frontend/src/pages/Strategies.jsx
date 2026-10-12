import { useEffect, useState } from 'react';
import { Shield, Plus, Play, FileCode2, Clock, CheckCircle2, XCircle, X } from 'lucide-react';

export default function Strategies() {
    const [strategies, setStrategies] = useState([]);
    const [databases, setDatabases] = useState([]);
    const [loading, setLoading] = useState(true);

    // Estados para el Modal de creación
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [formData, setFormData] = useState({
        name: '',
        databaseTargetId: '',
        retentionDays: 7,
        description: '',
        responsible: '',
        destinationPath: './backups',
        active: true,
        backupTypes: [
            { backupType: 'FULL', compressed: true, parameters: '' }
        ]
    });

    const fetchData = () => {
        Promise.all([
            fetch('http://localhost:8080/api/strategies').then(res => res.json()),
            fetch('http://localhost:8080/api/databases').then(res => res.json())
        ])
            .then(([stratData, dbData]) => {
                setStrategies(stratData);
                setDatabases(dbData);
                if (dbData.length > 0) {
                    setFormData(prev => ({ ...prev, databaseTargetId: dbData[0].id }));
                }
                setLoading(false);
            })
            .catch((err) => {
                console.error("Error al cargar datos:", err);
                setLoading(false);
            });
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleRun = (id, name) => {
        if (window.confirm(`¿Deseas iniciar un respaldo manual (Tipo FULL) para la estrategia '${name}'?`)) {
            fetch(`http://localhost:8080/api/strategies/${id}/run?backupType=FULL`, {
                method: 'POST'
            })
                .then(() => alert('✅ Ejecución iniciada. Revisa la pestaña de Historial para ver el progreso.'))
                .catch((err) => console.error("Error al ejecutar:", err));
        }
    };

    const handlePreviewScript = (id) => {
        fetch(`http://localhost:8080/api/strategies/${id}/script?backupType=FULL`)
            .then((res) => res.json())
            .then((data) => {
                alert("VISTA PREVIA SCRIPT RMAN:\n\n" + data.script);
            })
            .catch((err) => console.error("Error al previsualizar:", err));
    };

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : (type === 'number' ? Number(value) : value)
        }));
    };

    const handleBackupTypeChange = (index, field, value) => {
        const updatedBackupTypes = [...formData.backupTypes];
        updatedBackupTypes[index][field] = value;
        setFormData(prev => ({ ...prev, backupTypes: updatedBackupTypes }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        setIsSubmitting(true);

        fetch('http://localhost:8080/api/strategies', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        })
            .then(async (res) => {
                if (!res.ok) {
                    const errorData = await res.json().catch(() => ({}));
                    throw new Error(errorData.message || 'Error al registrar la estrategia');
                }
                return res.json();
            })
            .then(() => {
                setIsModalOpen(false);
                fetchData();
            })
            .catch((err) => alert(err.message))
            .finally(() => setIsSubmitting(false));
    };

    return (
        <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500">
            <div className="flex justify-between items-end">
                <div>
                    <h2 className="text-3xl font-semibold tracking-tight text-gray-900">Estrategias de Respaldo</h2>
                    <p className="text-gray-500 mt-2">Configure qué, cómo y cuándo se respaldan sus bases de datos.</p>
                </div>
                <button
                    onClick={() => setIsModalOpen(true)}
                    className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2.5 rounded-xl font-medium transition-colors shadow-sm"
                >
                    <Plus size={20} />
                    Nueva Estrategia
                </button>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                {loading ? (
                    <div className="flex justify-center py-12">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                    </div>
                ) : strategies.length === 0 ? (
                    <div className="text-center py-12 text-gray-500">No hay estrategias configuradas.</div>
                ) : (
                    <table className="w-full text-left border-collapse">
                        <thead>
                        <tr className="bg-gray-50/50 border-b border-gray-100">
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Estrategia</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Retención</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500">Estado</th>
                            <th className="px-6 py-4 text-sm font-medium text-gray-500 text-right">Acciones Rápidas</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                        {strategies.map((st) => (
                            <tr key={st.id} className="hover:bg-gray-50/50 transition-colors">
                                <td className="px-6 py-4">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg">
                                            <Shield size={20} />
                                        </div>
                                        <div>
                                            <div className="font-semibold text-gray-900">{st.name}</div>
                                            <div className="text-sm text-gray-500 mt-0.5">{st.description}</div>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center gap-1.5 text-gray-700 font-medium">
                                        <Clock size={16} className="text-gray-400" />
                                        {st.retentionDays} días
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    {st.active ? (
                                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-green-50 text-green-700 border border-green-100">
                        <CheckCircle2 size={14} /> Activa
                      </span>
                                    ) : (
                                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-700 border border-gray-200">
                        <XCircle size={14} /> Inactiva
                      </span>
                                    )}
                                </td>
                                <td className="px-6 py-4 text-right">
                                    <div className="flex justify-end gap-2">
                                        <button
                                            onClick={() => handlePreviewScript(st.id)}
                                            className="inline-flex items-center gap-2 text-sm font-medium text-gray-600 hover:text-gray-900 bg-gray-50 hover:bg-gray-100 px-3 py-1.5 rounded-lg transition-colors"
                                            title="Ver Script RMAN"
                                        >
                                            <FileCode2 size={16} />
                                            Script
                                        </button>
                                        <button
                                            onClick={() => handleRun(st.id, st.name)}
                                            className="inline-flex items-center gap-2 text-sm font-medium text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-lg transition-colors"
                                        >
                                            <Play size={16} fill="currentColor" />
                                            Ejecutar
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Modal para Crear Estrategia */}
            {isModalOpen && (
                <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex justify-center items-center p-4">
                    <div className="bg-white rounded-2xl w-full max-w-xl shadow-xl overflow-hidden animate-in zoom-in-95 duration-200 max-h-[90vh] flex flex-col">
                        <div className="flex justify-between items-center p-6 border-b border-gray-100">
                            <h3 className="text-lg font-semibold text-gray-900">Nueva Estrategia de Respaldo</h3>
                            <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 transition-colors">
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="p-6 space-y-4 overflow-y-auto flex-1">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Nombre de la Estrategia</label>
                                <input type="text" name="name" required value={formData.name} onChange={handleInputChange} placeholder="Ej. Respaldo Semanal Completo"
                                       className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all" />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Base de Datos Objetivo</label>
                                <select name="databaseTargetId" required value={formData.databaseTargetId} onChange={handleInputChange}
                                        className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white transition-all">
                                    {databases.map(db => (
                                        <option key={db.id} value={db.id}>{db.name} ({db.host})</option>
                                    ))}
                                </select>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Retención (Días)</label>
                                    <input type="number" name="retentionDays" required min="1" value={formData.retentionDays} onChange={handleInputChange}
                                           className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all" />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Responsable</label>
                                    <input type="text" name="responsible" value={formData.responsible} onChange={handleInputChange} placeholder="Ej. DBA Team"
                                           className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all" />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Ruta de Destino (Backup Path)</label>
                                <input type="text" name="destinationPath" required value={formData.destinationPath} onChange={handleInputChange}
                                       className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all" />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Descripción</label>
                                <textarea name="description" rows="2" value={formData.description} onChange={handleInputChange} placeholder="Detalles de la estrategia..."
                                          className="w-full px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all resize-none"></textarea>
                            </div>

                            <div className="border-t border-gray-100 pt-4">
                                <label className="block text-sm font-medium text-gray-700 mb-2">Configuración de Tipos de Respaldo</label>
                                <div className="bg-gray-50 p-4 rounded-xl space-y-3">
                                    <div className="grid grid-cols-2 gap-3">
                                        <div>
                                            <label className="block text-xs text-gray-500 mb-1">Tipo</label>
                                            <select
                                                value={formData.backupTypes[0].backupType}
                                                onChange={(e) => handleBackupTypeChange(0, 'backupType', e.target.value)}
                                                className="w-full px-3 py-1.5 border border-gray-200 rounded-lg text-sm bg-white"
                                            >
                                                <option value="FULL">FULL</option>
                                                <option value="INC_0">INC_0</option>
                                                <option value="INC_1_DIF">INC_1_DIF</option>
                                                <option value="INC_1_ACUM">INC_1_ACUM</option>
                                                <option value="ARCHIVELOG">ARCHIVELOG</option>
                                            </select>
                                        </div>
                                        <div className="flex items-center pt-5">
                                            <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={formData.backupTypes[0].compressed}
                                                    onChange={(e) => handleBackupTypeChange(0, 'compressed', e.target.checked)}
                                                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500 w-4 h-4"
                                                />
                                                Comprimido
                                            </label>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="pt-4 flex justify-end gap-3 border-t border-gray-100">
                                <button type="button" onClick={() => setIsModalOpen(false)}
                                        className="px-4 py-2 text-gray-700 bg-gray-50 hover:bg-gray-100 rounded-xl font-medium transition-colors">
                                    Cancelar
                                </button>
                                <button type="submit" disabled={isSubmitting}
                                        className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-medium transition-colors shadow-sm disabled:opacity-50">
                                    {isSubmitting ? 'Guardando...' : 'Crear Estrategia'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}