/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.apiclient;

import io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal.ElasticsearchEndpointDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public final class ElasticsearchEndpointMap {

  private static final Map<String, ElasticsearchEndpointDefinition> routesMap;

  static {
    Map<String, ElasticsearchEndpointDefinition> routes = new HashMap<>(415);
    initEndpoint(routes, "async_search.status", false, "/_async_search/status/{id}");
    initEndpoint(routes, "indices.analyze", false, "/_analyze", "/{index}/_analyze");
    initEndpoint(routes, "sql.clear_cursor", false, "/_sql/close");
    initEndpoint(routes, "ml.delete_datafeed", false, "/_ml/datafeeds/{datafeed_id}");
    initEndpoint(routes, "explain", false, "/{index}/_explain/{id}");
    initEndpoint(
        routes,
        "cat.thread_pool",
        false,
        "/_cat/thread_pool",
        "/_cat/thread_pool/{thread_pool_patterns}");
    initEndpoint(routes, "ml.delete_calendar", false, "/_ml/calendars/{calendar_id}");
    initEndpoint(routes, "indices.create_data_stream", false, "/_data_stream/{name}");
    initEndpoint(routes, "cat.fielddata", false, "/_cat/fielddata", "/_cat/fielddata/{fields}");
    initEndpoint(routes, "security.enroll_node", false, "/_security/enroll/node");
    initEndpoint(routes, "slm.get_status", false, "/_slm/status");
    initEndpoint(routes, "ml.put_calendar", false, "/_ml/calendars/{calendar_id}");
    initEndpoint(routes, "create", false, "/{index}/_create/{id}");
    initEndpoint(
        routes,
        "ml.preview_datafeed",
        false,
        "/_ml/datafeeds/{datafeed_id}/_preview",
        "/_ml/datafeeds/_preview");
    initEndpoint(routes, "indices.put_template", false, "/_template/{name}");
    initEndpoint(
        routes,
        "nodes.reload_secure_settings",
        false,
        "/_nodes/reload_secure_settings",
        "/_nodes/{node_id}/reload_secure_settings");
    initEndpoint(routes, "indices.delete_data_stream", false, "/_data_stream/{name}");
    initEndpoint(
        routes,
        "transform.schedule_now_transform",
        false,
        "/_transform/{transform_id}/_schedule_now");
    initEndpoint(routes, "slm.stop", false, "/_slm/stop");
    initEndpoint(routes, "rollup.delete_job", false, "/_rollup/job/{id}");
    initEndpoint(routes, "cluster.put_component_template", false, "/_component_template/{name}");
    initEndpoint(routes, "delete_script", false, "/_scripts/{id}");
    initEndpoint(routes, "ml.delete_trained_model", false, "/_ml/trained_models/{model_id}");
    initEndpoint(
        routes,
        "indices.simulate_template",
        false,
        "/_index_template/_simulate",
        "/_index_template/_simulate/{name}");
    initEndpoint(routes, "slm.get_lifecycle", false, "/_slm/policy/{policy_id}", "/_slm/policy");
    initEndpoint(routes, "security.enroll_kibana", false, "/_security/enroll/kibana");
    initEndpoint(routes, "fleet.search", false, "/{index}/_fleet/_fleet_search");
    initEndpoint(routes, "reindex_rethrottle", false, "/_reindex/{task_id}/_rethrottle");
    initEndpoint(routes, "ml.update_filter", false, "/_ml/filters/{filter_id}/_update");
    initEndpoint(routes, "rollup.get_rollup_caps", false, "/_rollup/data/{id}", "/_rollup/data");
    initEndpoint(
        routes, "ccr.resume_auto_follow_pattern", false, "/_ccr/auto_follow/{name}/resume");
    initEndpoint(routes, "features.get_features", false, "/_features");
    initEndpoint(routes, "slm.get_stats", false, "/_slm/stats");
    initEndpoint(routes, "indices.clear_cache", false, "/_cache/clear", "/{index}/_cache/clear");
    initEndpoint(
        routes,
        "cluster.post_voting_config_exclusions",
        false,
        "/_cluster/voting_config_exclusions");
    initEndpoint(routes, "index", false, "/{index}/_doc/{id}", "/{index}/_doc");
    initEndpoint(routes, "cat.pending_tasks", false, "/_cat/pending_tasks");
    initEndpoint(routes, "indices.promote_data_stream", false, "/_data_stream/_promote/{name}");
    initEndpoint(routes, "ml.delete_filter", false, "/_ml/filters/{filter_id}");
    initEndpoint(routes, "sql.query", false, "/_sql");
    initEndpoint(routes, "ccr.follow_stats", false, "/{index}/_ccr/stats");
    initEndpoint(routes, "transform.stop_transform", false, "/_transform/{transform_id}/_stop");
    initEndpoint(
        routes,
        "security.has_privileges_user_profile",
        false,
        "/_security/profile/_has_privileges");
    initEndpoint(
        routes, "autoscaling.delete_autoscaling_policy", false, "/_autoscaling/policy/{name}");
    initEndpoint(routes, "scripts_painless_execute", false, "/_scripts/painless/_execute");
    initEndpoint(routes, "indices.delete", false, "/{index}");
    initEndpoint(
        routes, "security.clear_cached_roles", false, "/_security/role/{name}/_clear_cache");
    initEndpoint(routes, "eql.delete", false, "/_eql/search/{id}");
    initEndpoint(routes, "update", false, "/{index}/_update/{id}");
    initEndpoint(
        routes,
        "snapshot.clone",
        false,
        "/_snapshot/{repository}/{snapshot}/_clone/{target_snapshot}");
    initEndpoint(routes, "license.get_basic_status", false, "/_license/basic_status");
    initEndpoint(routes, "indices.close", false, "/{index}/_close");
    initEndpoint(routes, "security.saml_authenticate", false, "/_security/saml/authenticate");
    initEndpoint(
        routes, "search_application.put", false, "/_application/search_application/{name}");
    initEndpoint(routes, "count", false, "/_count", "/{index}/_count");
    initEndpoint(
        routes,
        "migration.deprecations",
        false,
        "/_migration/deprecations",
        "/{index}/_migration/deprecations");
    initEndpoint(routes, "indices.segments", false, "/_segments", "/{index}/_segments");
    initEndpoint(routes, "security.suggest_user_profiles", false, "/_security/profile/_suggest");
    initEndpoint(routes, "security.get_user_privileges", false, "/_security/user/_privileges");
    initEndpoint(
        routes,
        "indices.delete_alias",
        false,
        "/{index}/_alias/{name}",
        "/{index}/_aliases/{name}");
    initEndpoint(routes, "indices.get_mapping", false, "/_mapping", "/{index}/_mapping");
    initEndpoint(routes, "indices.put_index_template", false, "/_index_template/{name}");
    initEndpoint(
        routes,
        "searchable_snapshots.stats",
        false,
        "/_searchable_snapshots/stats",
        "/{index}/_searchable_snapshots/stats");
    initEndpoint(routes, "security.disable_user", false, "/_security/user/{username}/_disable");
    initEndpoint(
        routes,
        "ml.upgrade_job_snapshot",
        false,
        "/_ml/anomaly_detectors/{job_id}/model_snapshots/{snapshot_id}/_upgrade");
    initEndpoint(routes, "delete", false, "/{index}/_doc/{id}");
    initEndpoint(routes, "async_search.delete", false, "/_async_search/{id}");
    initEndpoint(
        routes, "cat.transforms", false, "/_cat/transforms", "/_cat/transforms/{transform_id}");
    initEndpoint(routes, "ping", false, "/");
    initEndpoint(routes, "ccr.pause_auto_follow_pattern", false, "/_ccr/auto_follow/{name}/pause");
    initEndpoint(routes, "indices.shard_stores", false, "/_shard_stores", "/{index}/_shard_stores");
    initEndpoint(
        routes, "ml.update_data_frame_analytics", false, "/_ml/data_frame/analytics/{id}/_update");
    initEndpoint(routes, "logstash.delete_pipeline", false, "/_logstash/pipeline/{id}");
    initEndpoint(routes, "sql.translate", false, "/_sql/translate");
    initEndpoint(routes, "exists", false, "/{index}/_doc/{id}");
    initEndpoint(routes, "snapshot.get_repository", false, "/_snapshot", "/_snapshot/{repository}");
    initEndpoint(routes, "snapshot.verify_repository", false, "/_snapshot/{repository}/_verify");
    initEndpoint(routes, "indices.put_data_lifecycle", false, "/_data_stream/{name}/_lifecycle");
    initEndpoint(routes, "ml.open_job", false, "/_ml/anomaly_detectors/{job_id}/_open");
    initEndpoint(
        routes, "security.update_user_profile_data", false, "/_security/profile/{uid}/_data");
    initEndpoint(routes, "enrich.put_policy", false, "/_enrich/policy/{name}");
    initEndpoint(
        routes,
        "ml.get_datafeed_stats",
        false,
        "/_ml/datafeeds/{datafeed_id}/_stats",
        "/_ml/datafeeds/_stats");
    initEndpoint(routes, "open_point_in_time", false, "/{index}/_pit");
    initEndpoint(routes, "get_source", false, "/{index}/_source/{id}");
    initEndpoint(routes, "delete_by_query", false, "/{index}/_delete_by_query");
    initEndpoint(routes, "security.create_api_key", false, "/_security/api_key");
    initEndpoint(routes, "cat.tasks", false, "/_cat/tasks");
    initEndpoint(routes, "watcher.delete_watch", false, "/_watcher/watch/{id}");
    initEndpoint(routes, "ingest.processor_grok", false, "/_ingest/processor/grok");
    initEndpoint(routes, "ingest.put_pipeline", false, "/_ingest/pipeline/{id}");
    initEndpoint(
        routes,
        "ml.get_data_frame_analytics_stats",
        false,
        "/_ml/data_frame/analytics/_stats",
        "/_ml/data_frame/analytics/{id}/_stats");
    initEndpoint(
        routes,
        "indices.data_streams_stats",
        false,
        "/_data_stream/_stats",
        "/_data_stream/{name}/_stats");
    initEndpoint(
        routes, "security.clear_cached_realms", false, "/_security/realm/{realms}/_clear_cache");
    initEndpoint(routes, "field_caps", false, "/_field_caps", "/{index}/_field_caps");
    initEndpoint(routes, "ml.evaluate_data_frame", false, "/_ml/data_frame/_evaluate");
    initEndpoint(
        routes,
        "ml.delete_forecast",
        false,
        "/_ml/anomaly_detectors/{job_id}/_forecast",
        "/_ml/anomaly_detectors/{job_id}/_forecast/{forecast_id}");
    initEndpoint(routes, "enrich.get_policy", false, "/_enrich/policy/{name}", "/_enrich/policy");
    initEndpoint(routes, "rollup.start_job", false, "/_rollup/job/{id}/_start");
    initEndpoint(routes, "tasks.cancel", false, "/_tasks/_cancel", "/_tasks/{task_id}/_cancel");
    initEndpoint(routes, "security.saml_logout", false, "/_security/saml/logout");
    initEndpoint(
        routes, "render_search_template", true, "/_render/template", "/_render/template/{id}");
    initEndpoint(routes, "ml.get_calendar_events", false, "/_ml/calendars/{calendar_id}/events");
    initEndpoint(routes, "security.enable_user_profile", false, "/_security/profile/{uid}/_enable");
    initEndpoint(
        routes, "logstash.get_pipeline", false, "/_logstash/pipeline", "/_logstash/pipeline/{id}");
    initEndpoint(routes, "cat.snapshots", false, "/_cat/snapshots", "/_cat/snapshots/{repository}");
    initEndpoint(routes, "indices.add_block", false, "/{index}/_block/{block}");
    initEndpoint(routes, "terms_enum", true, "/{index}/_terms_enum");
    initEndpoint(routes, "ml.forecast", false, "/_ml/anomaly_detectors/{job_id}/_forecast");
    initEndpoint(
        routes, "cluster.stats", false, "/_cluster/stats", "/_cluster/stats/nodes/{node_id}");
    initEndpoint(routes, "search_application.list", false, "/_application/search_application");
    initEndpoint(routes, "cat.count", false, "/_cat/count", "/_cat/count/{index}");
    initEndpoint(routes, "cat.segments", false, "/_cat/segments", "/_cat/segments/{index}");
    initEndpoint(routes, "ccr.resume_follow", false, "/{index}/_ccr/resume_follow");
    initEndpoint(
        routes, "search_application.get", false, "/_application/search_application/{name}");
    initEndpoint(
        routes,
        "security.saml_service_provider_metadata",
        false,
        "/_security/saml/metadata/{realm_name}");
    initEndpoint(routes, "update_by_query", false, "/{index}/_update_by_query");
    initEndpoint(routes, "ml.stop_datafeed", false, "/_ml/datafeeds/{datafeed_id}/_stop");
    initEndpoint(routes, "ilm.explain_lifecycle", false, "/{index}/_ilm/explain");
    initEndpoint(
        routes,
        "ml.put_trained_model_vocabulary",
        false,
        "/_ml/trained_models/{model_id}/vocabulary");
    initEndpoint(routes, "indices.exists", false, "/{index}");
    initEndpoint(routes, "ml.set_upgrade_mode", false, "/_ml/set_upgrade_mode");
    initEndpoint(routes, "security.saml_invalidate", false, "/_security/saml/invalidate");
    initEndpoint(
        routes,
        "ml.get_job_stats",
        false,
        "/_ml/anomaly_detectors/_stats",
        "/_ml/anomaly_detectors/{job_id}/_stats");
    initEndpoint(routes, "cluster.allocation_explain", false, "/_cluster/allocation/explain");
    initEndpoint(routes, "watcher.activate_watch", false, "/_watcher/watch/{watch_id}/_activate");
    initEndpoint(
        routes,
        "searchable_snapshots.clear_cache",
        false,
        "/_searchable_snapshots/cache/clear",
        "/{index}/_searchable_snapshots/cache/clear");
    initEndpoint(
        routes, "msearch_template", true, "/_msearch/template", "/{index}/_msearch/template");
    initEndpoint(routes, "bulk", false, "/_bulk", "/{index}/_bulk");
    initEndpoint(routes, "cat.nodeattrs", false, "/_cat/nodeattrs");
    initEndpoint(
        routes, "indices.get_index_template", false, "/_index_template", "/_index_template/{name}");
    initEndpoint(routes, "license.get", false, "/_license");
    initEndpoint(routes, "ccr.forget_follower", false, "/{index}/_ccr/forget_follower");
    initEndpoint(routes, "security.delete_role", false, "/_security/role/{name}");
    initEndpoint(
        routes, "indices.validate_query", false, "/_validate/query", "/{index}/_validate/query");
    initEndpoint(routes, "tasks.get", false, "/_tasks/{task_id}");
    initEndpoint(
        routes, "ml.start_data_frame_analytics", false, "/_ml/data_frame/analytics/{id}/_start");
    initEndpoint(routes, "indices.create", false, "/{index}");
    initEndpoint(
        routes,
        "cluster.delete_voting_config_exclusions",
        false,
        "/_cluster/voting_config_exclusions");
    initEndpoint(routes, "info", false, "/");
    initEndpoint(routes, "watcher.stop", false, "/_watcher/_stop");
    initEndpoint(routes, "enrich.delete_policy", false, "/_enrich/policy/{name}");
    initEndpoint(
        routes,
        "cat.ml_data_frame_analytics",
        false,
        "/_cat/ml/data_frame/analytics",
        "/_cat/ml/data_frame/analytics/{id}");
    initEndpoint(
        routes,
        "security.change_password",
        false,
        "/_security/user/{username}/_password",
        "/_security/user/_password");
    initEndpoint(routes, "put_script", false, "/_scripts/{id}", "/_scripts/{id}/{context}");
    initEndpoint(routes, "ml.put_datafeed", false, "/_ml/datafeeds/{datafeed_id}");
    initEndpoint(routes, "cat.master", false, "/_cat/master");
    initEndpoint(routes, "features.reset_features", false, "/_features/_reset");
    initEndpoint(routes, "indices.get_data_lifecycle", false, "/_data_stream/{name}/_lifecycle");
    initEndpoint(
        routes,
        "ml.get_data_frame_analytics",
        false,
        "/_ml/data_frame/analytics/{id}",
        "/_ml/data_frame/analytics");
    initEndpoint(
        routes,
        "security.delete_service_token",
        false,
        "/_security/service/{namespace}/{service}/credential/token/{name}");
    initEndpoint(routes, "indices.recovery", false, "/_recovery", "/{index}/_recovery");
    initEndpoint(routes, "cat.recovery", false, "/_cat/recovery", "/_cat/recovery/{index}");
    initEndpoint(routes, "indices.downsample", false, "/{index}/_downsample/{target_index}");
    initEndpoint(routes, "ingest.delete_pipeline", false, "/_ingest/pipeline/{id}");
    initEndpoint(routes, "async_search.get", false, "/_async_search/{id}");
    initEndpoint(routes, "eql.get", false, "/_eql/search/{id}");
    initEndpoint(routes, "cat.aliases", false, "/_cat/aliases", "/_cat/aliases/{name}");
    initEndpoint(
        routes,
        "security.get_service_credentials",
        false,
        "/_security/service/{namespace}/{service}/credential");
    initEndpoint(routes, "cat.allocation", false, "/_cat/allocation", "/_cat/allocation/{node_id}");
    initEndpoint(
        routes, "ml.stop_data_frame_analytics", false, "/_ml/data_frame/analytics/{id}/_stop");
    initEndpoint(routes, "indices.open", false, "/{index}/_open");
    initEndpoint(routes, "ilm.get_lifecycle", false, "/_ilm/policy/{policy}", "/_ilm/policy");
    initEndpoint(routes, "ilm.remove_policy", false, "/{index}/_ilm/remove");
    initEndpoint(
        routes,
        "security.get_role_mapping",
        false,
        "/_security/role_mapping/{name}",
        "/_security/role_mapping");
    initEndpoint(routes, "snapshot.create", false, "/_snapshot/{repository}/{snapshot}");
    initEndpoint(routes, "watcher.get_watch", false, "/_watcher/watch/{id}");
    initEndpoint(routes, "license.post_start_trial", false, "/_license/start_trial");
    initEndpoint(routes, "snapshot.restore", false, "/_snapshot/{repository}/{snapshot}/_restore");
    initEndpoint(routes, "indices.put_mapping", false, "/{index}/_mapping");
    initEndpoint(
        routes, "ml.delete_calendar_job", false, "/_ml/calendars/{calendar_id}/jobs/{job_id}");
    initEndpoint(
        routes, "security.clear_api_key_cache", false, "/_security/api_key/{ids}/_clear_cache");
    initEndpoint(routes, "slm.start", false, "/_slm/start");
    initEndpoint(
        routes,
        "cat.component_templates",
        false,
        "/_cat/component_templates",
        "/_cat/component_templates/{name}");
    initEndpoint(routes, "security.enable_user", false, "/_security/user/{username}/_enable");
    initEndpoint(routes, "cluster.delete_component_template", false, "/_component_template/{name}");
    initEndpoint(routes, "security.get_role", false, "/_security/role/{name}", "/_security/role");
    initEndpoint(
        routes, "ingest.get_pipeline", false, "/_ingest/pipeline", "/_ingest/pipeline/{id}");
    initEndpoint(
        routes,
        "ml.delete_expired_data",
        false,
        "/_ml/_delete_expired_data/{job_id}",
        "/_ml/_delete_expired_data");
    initEndpoint(
        routes,
        "indices.get_settings",
        false,
        "/_settings",
        "/{index}/_settings",
        "/{index}/_settings/{name}",
        "/_settings/{name}");
    initEndpoint(routes, "ccr.follow", false, "/{index}/_ccr/follow");
    initEndpoint(
        routes, "termvectors", false, "/{index}/_termvectors/{id}", "/{index}/_termvectors");
    initEndpoint(routes, "ml.post_data", false, "/_ml/anomaly_detectors/{job_id}/_data");
    initEndpoint(routes, "eql.search", true, "/{index}/_eql/search");
    initEndpoint(
        routes,
        "ml.get_trained_models",
        false,
        "/_ml/trained_models/{model_id}",
        "/_ml/trained_models");
    initEndpoint(
        routes, "security.disable_user_profile", false, "/_security/profile/{uid}/_disable");
    initEndpoint(routes, "security.put_privileges", false, "/_security/privilege");
    initEndpoint(routes, "cat.nodes", false, "/_cat/nodes");
    initEndpoint(
        routes, "nodes.info", false, "/_nodes", "/_nodes/{node_id}", "/_nodes/{node_id}/{metric}");
    initEndpoint(routes, "graph.explore", false, "/{index}/_graph/explore");
    initEndpoint(
        routes, "autoscaling.put_autoscaling_policy", false, "/_autoscaling/policy/{name}");
    initEndpoint(routes, "cat.templates", false, "/_cat/templates", "/_cat/templates/{name}");
    initEndpoint(routes, "cluster.remote_info", false, "/_remote/info");
    initEndpoint(routes, "rank_eval", false, "/_rank_eval", "/{index}/_rank_eval");
    initEndpoint(
        routes, "security.delete_privileges", false, "/_security/privilege/{application}/{name}");
    initEndpoint(
        routes,
        "security.get_privileges",
        false,
        "/_security/privilege",
        "/_security/privilege/{application}",
        "/_security/privilege/{application}/{name}");
    initEndpoint(routes, "scroll", false, "/_search/scroll");
    initEndpoint(routes, "license.delete", false, "/_license");
    initEndpoint(routes, "indices.disk_usage", false, "/{index}/_disk_usage");
    initEndpoint(routes, "msearch", true, "/_msearch", "/{index}/_msearch");
    initEndpoint(routes, "indices.field_usage_stats", false, "/{index}/_field_usage_stats");
    initEndpoint(
        routes, "indices.rollover", false, "/{alias}/_rollover", "/{alias}/_rollover/{new_index}");
    initEndpoint(
        routes,
        "cat.ml_trained_models",
        false,
        "/_cat/ml/trained_models",
        "/_cat/ml/trained_models/{model_id}");
    initEndpoint(
        routes,
        "ml.delete_trained_model_alias",
        false,
        "/_ml/trained_models/{model_id}/model_aliases/{model_alias}");
    initEndpoint(routes, "indices.get", false, "/{index}");
    initEndpoint(routes, "sql.get_async_status", false, "/_sql/async/status/{id}");
    initEndpoint(routes, "ilm.stop", false, "/_ilm/stop");
    initEndpoint(routes, "security.put_user", false, "/_security/user/{username}");
    initEndpoint(
        routes,
        "cluster.state",
        false,
        "/_cluster/state",
        "/_cluster/state/{metric}",
        "/_cluster/state/{metric}/{index}");
    initEndpoint(routes, "indices.put_settings", false, "/_settings", "/{index}/_settings");
    initEndpoint(routes, "knn_search", false, "/{index}/_knn_search");
    initEndpoint(routes, "get", false, "/{index}/_doc/{id}");
    initEndpoint(routes, "eql.get_status", false, "/_eql/search/status/{id}");
    initEndpoint(routes, "ssl.certificates", false, "/_ssl/certificates");
    initEndpoint(
        routes,
        "ml.get_model_snapshots",
        false,
        "/_ml/anomaly_detectors/{job_id}/model_snapshots/{snapshot_id}",
        "/_ml/anomaly_detectors/{job_id}/model_snapshots");
    initEndpoint(
        routes,
        "nodes.clear_repositories_metering_archive",
        false,
        "/_nodes/{node_id}/_repositories_metering/{max_archive_version}");
    initEndpoint(routes, "security.put_role", false, "/_security/role/{name}");
    initEndpoint(
        routes, "ml.get_influencers", false, "/_ml/anomaly_detectors/{job_id}/results/influencers");
    initEndpoint(routes, "transform.upgrade_transforms", false, "/_transform/_upgrade");
    initEndpoint(
        routes,
        "ml.delete_calendar_event",
        false,
        "/_ml/calendars/{calendar_id}/events/{event_id}");
    initEndpoint(
        routes,
        "indices.get_field_mapping",
        false,
        "/_mapping/field/{fields}",
        "/{index}/_mapping/field/{fields}");
    initEndpoint(
        routes,
        "transform.preview_transform",
        false,
        "/_transform/{transform_id}/_preview",
        "/_transform/_preview");
    initEndpoint(routes, "tasks.list", false, "/_tasks");
    initEndpoint(
        routes,
        "ml.clear_trained_model_deployment_cache",
        false,
        "/_ml/trained_models/{model_id}/deployment/cache/_clear");
    initEndpoint(routes, "cluster.reroute", false, "/_cluster/reroute");
    initEndpoint(routes, "security.saml_complete_logout", false, "/_security/saml/complete_logout");
    initEndpoint(
        routes,
        "indices.simulate_index_template",
        false,
        "/_index_template/_simulate_index/{name}");
    initEndpoint(routes, "snapshot.get", false, "/_snapshot/{repository}/{snapshot}");
    initEndpoint(routes, "ccr.put_auto_follow_pattern", false, "/_ccr/auto_follow/{name}");
    initEndpoint(
        routes, "nodes.hot_threads", false, "/_nodes/hot_threads", "/_nodes/{node_id}/hot_threads");
    initEndpoint(
        routes,
        "ml.preview_data_frame_analytics",
        false,
        "/_ml/data_frame/analytics/_preview",
        "/_ml/data_frame/analytics/{id}/_preview");
    initEndpoint(routes, "indices.flush", false, "/_flush", "/{index}/_flush");
    initEndpoint(routes, "cluster.exists_component_template", false, "/_component_template/{name}");
    initEndpoint(
        routes,
        "snapshot.status",
        false,
        "/_snapshot/_status",
        "/_snapshot/{repository}/_status",
        "/_snapshot/{repository}/{snapshot}/_status");
    initEndpoint(routes, "ml.update_datafeed", false, "/_ml/datafeeds/{datafeed_id}/_update");
    initEndpoint(routes, "indices.update_aliases", false, "/_aliases");
    initEndpoint(routes, "autoscaling.get_autoscaling_capacity", false, "/_autoscaling/capacity");
    initEndpoint(routes, "migration.post_feature_upgrade", false, "/_migration/system_features");
    initEndpoint(
        routes, "ml.get_records", false, "/_ml/anomaly_detectors/{job_id}/results/records");
    initEndpoint(
        routes,
        "indices.get_alias",
        false,
        "/_alias",
        "/_alias/{name}",
        "/{index}/_alias/{name}",
        "/{index}/_alias");
    initEndpoint(routes, "logstash.put_pipeline", false, "/_logstash/pipeline/{id}");
    initEndpoint(routes, "snapshot.delete_repository", false, "/_snapshot/{repository}");
    initEndpoint(
        routes,
        "security.has_privileges",
        false,
        "/_security/user/_has_privileges",
        "/_security/user/{user}/_has_privileges");
    initEndpoint(routes, "cat.indices", false, "/_cat/indices", "/_cat/indices/{index}");
    initEndpoint(
        routes,
        "ccr.get_auto_follow_pattern",
        false,
        "/_ccr/auto_follow",
        "/_ccr/auto_follow/{name}");
    initEndpoint(routes, "ml.start_datafeed", false, "/_ml/datafeeds/{datafeed_id}/_start");
    initEndpoint(routes, "indices.clone", false, "/{index}/_clone/{target}");
    initEndpoint(
        routes, "search_application.delete", false, "/_application/search_application/{name}");
    initEndpoint(routes, "security.query_api_keys", false, "/_security/_query/api_key");
    initEndpoint(routes, "ml.flush_job", false, "/_ml/anomaly_detectors/{job_id}/_flush");
    initEndpoint(
        routes,
        "security.clear_cached_privileges",
        false,
        "/_security/privilege/{application}/_clear_cache");
    initEndpoint(routes, "indices.exists_index_template", false, "/_index_template/{name}");
    initEndpoint(routes, "indices.explain_data_lifecycle", false, "/{index}/_lifecycle/explain");
    initEndpoint(
        routes, "indices.put_alias", false, "/{index}/_alias/{name}", "/{index}/_aliases/{name}");
    initEndpoint(
        routes,
        "ml.get_buckets",
        false,
        "/_ml/anomaly_detectors/{job_id}/results/buckets/{timestamp}",
        "/_ml/anomaly_detectors/{job_id}/results/buckets");
    initEndpoint(
        routes,
        "ml.put_trained_model_definition_part",
        false,
        "/_ml/trained_models/{model_id}/definition/{part}");
    initEndpoint(routes, "get_script", false, "/_scripts/{id}");
    initEndpoint(
        routes,
        "ingest.simulate",
        false,
        "/_ingest/pipeline/_simulate",
        "/_ingest/pipeline/{id}/_simulate");
    initEndpoint(routes, "indices.migrate_to_data_stream", false, "/_data_stream/_migrate/{name}");
    initEndpoint(routes, "enrich.execute_policy", false, "/_enrich/policy/{name}/_execute");
    initEndpoint(routes, "indices.split", false, "/{index}/_split/{target}");
    initEndpoint(
        routes,
        "ml.delete_model_snapshot",
        false,
        "/_ml/anomaly_detectors/{job_id}/model_snapshots/{snapshot_id}");
    initEndpoint(
        routes,
        "nodes.usage",
        false,
        "/_nodes/usage",
        "/_nodes/{node_id}/usage",
        "/_nodes/usage/{metric}",
        "/_nodes/{node_id}/usage/{metric}");
    initEndpoint(routes, "cat.help", false, "/_cat");
    initEndpoint(
        routes, "ml.estimate_model_memory", false, "/_ml/anomaly_detectors/_estimate_model_memory");
    initEndpoint(routes, "exists_source", false, "/{index}/_source/{id}");
    initEndpoint(routes, "ml.put_data_frame_analytics", false, "/_ml/data_frame/analytics/{id}");
    initEndpoint(routes, "security.put_role_mapping", false, "/_security/role_mapping/{name}");
    initEndpoint(routes, "rollup.get_rollup_index_caps", false, "/{index}/_rollup/data");
    initEndpoint(routes, "transform.reset_transform", false, "/_transform/{transform_id}/_reset");
    initEndpoint(
        routes,
        "ml.infer_trained_model",
        false,
        "/_ml/trained_models/{model_id}/_infer",
        "/_ml/trained_models/{model_id}/deployment/_infer");
    initEndpoint(routes, "reindex", false, "/_reindex");
    initEndpoint(routes, "ml.put_trained_model", false, "/_ml/trained_models/{model_id}");
    initEndpoint(
        routes,
        "cat.ml_jobs",
        false,
        "/_cat/ml/anomaly_detectors",
        "/_cat/ml/anomaly_detectors/{job_id}");
    initEndpoint(
        routes,
        "search_application.search",
        false,
        "/_application/search_application/{name}/_search");
    initEndpoint(routes, "ilm.put_lifecycle", false, "/_ilm/policy/{policy}");
    initEndpoint(routes, "security.get_token", false, "/_security/oauth2/token");
    initEndpoint(routes, "ilm.move_to_step", false, "/_ilm/move/{index}");
    initEndpoint(routes, "search_template", true, "/_search/template", "/{index}/_search/template");
    initEndpoint(routes, "indices.delete_data_lifecycle", false, "/_data_stream/{name}/_lifecycle");
    initEndpoint(routes, "indices.get_data_stream", false, "/_data_stream", "/_data_stream/{name}");
    initEndpoint(routes, "ml.get_filters", false, "/_ml/filters", "/_ml/filters/{filter_id}");
    initEndpoint(
        routes,
        "cat.ml_datafeeds",
        false,
        "/_cat/ml/datafeeds",
        "/_cat/ml/datafeeds/{datafeed_id}");
    initEndpoint(routes, "rollup.rollup_search", false, "/{index}/_rollup_search");
    initEndpoint(routes, "ml.put_job", false, "/_ml/anomaly_detectors/{job_id}");
    initEndpoint(
        routes, "update_by_query_rethrottle", false, "/_update_by_query/{task_id}/_rethrottle");
    initEndpoint(routes, "indices.delete_index_template", false, "/_index_template/{name}");
    initEndpoint(
        routes, "indices.reload_search_analyzers", false, "/{index}/_reload_search_analyzers");
    initEndpoint(routes, "cluster.get_settings", false, "/_cluster/settings");
    initEndpoint(routes, "cluster.put_settings", false, "/_cluster/settings");
    initEndpoint(routes, "transform.put_transform", false, "/_transform/{transform_id}");
    initEndpoint(routes, "watcher.stats", false, "/_watcher/stats", "/_watcher/stats/{metric}");
    initEndpoint(routes, "ccr.delete_auto_follow_pattern", false, "/_ccr/auto_follow/{name}");
    initEndpoint(routes, "mtermvectors", false, "/_mtermvectors", "/{index}/_mtermvectors");
    initEndpoint(routes, "license.post", false, "/_license");
    initEndpoint(routes, "xpack.info", false, "/_xpack");
    initEndpoint(
        routes, "dangling_indices.import_dangling_index", false, "/_dangling/{index_uuid}");
    initEndpoint(
        routes,
        "nodes.get_repositories_metering_info",
        false,
        "/_nodes/{node_id}/_repositories_metering");
    initEndpoint(
        routes, "transform.get_transform_stats", false, "/_transform/{transform_id}/_stats");
    initEndpoint(routes, "mget", false, "/_mget", "/{index}/_mget");
    initEndpoint(routes, "security.get_builtin_privileges", false, "/_security/privilege/_builtin");
    initEndpoint(
        routes,
        "ml.update_model_snapshot",
        false,
        "/_ml/anomaly_detectors/{job_id}/model_snapshots/{snapshot_id}/_update");
    initEndpoint(routes, "ml.info", false, "/_ml/info");
    initEndpoint(routes, "indices.exists_template", false, "/_template/{name}");
    initEndpoint(
        routes,
        "watcher.ack_watch",
        false,
        "/_watcher/watch/{watch_id}/_ack",
        "/_watcher/watch/{watch_id}/_ack/{action_id}");
    initEndpoint(
        routes, "security.get_user", false, "/_security/user/{username}", "/_security/user");
    initEndpoint(
        routes, "shutdown.get_node", false, "/_nodes/shutdown", "/_nodes/{node_id}/shutdown");
    initEndpoint(routes, "watcher.start", false, "/_watcher/_start");
    initEndpoint(routes, "indices.shrink", false, "/{index}/_shrink/{target}");
    initEndpoint(routes, "license.post_start_basic", false, "/_license/start_basic");
    initEndpoint(routes, "xpack.usage", false, "/_xpack/usage");
    initEndpoint(routes, "ilm.delete_lifecycle", false, "/_ilm/policy/{policy}");
    initEndpoint(routes, "ccr.follow_info", false, "/{index}/_ccr/info");
    initEndpoint(
        routes, "ml.put_calendar_job", false, "/_ml/calendars/{calendar_id}/jobs/{job_id}");
    initEndpoint(routes, "rollup.put_job", false, "/_rollup/job/{id}");
    initEndpoint(routes, "clear_scroll", false, "/_search/scroll");
    initEndpoint(routes, "ml.delete_data_frame_analytics", false, "/_ml/data_frame/analytics/{id}");
    initEndpoint(routes, "security.get_api_key", false, "/_security/api_key");
    initEndpoint(routes, "cat.health", false, "/_cat/health");
    initEndpoint(routes, "security.invalidate_token", false, "/_security/oauth2/token");
    initEndpoint(routes, "slm.delete_lifecycle", false, "/_slm/policy/{policy_id}");
    initEndpoint(
        routes,
        "ml.stop_trained_model_deployment",
        false,
        "/_ml/trained_models/{model_id}/deployment/_stop");
    initEndpoint(routes, "monitoring.bulk", false, "/_monitoring/bulk", "/_monitoring/{type}/bulk");
    initEndpoint(
        routes,
        "indices.stats",
        false,
        "/_stats",
        "/_stats/{metric}",
        "/{index}/_stats",
        "/{index}/_stats/{metric}");
    initEndpoint(
        routes,
        "searchable_snapshots.cache_stats",
        false,
        "/_searchable_snapshots/cache/stats",
        "/_searchable_snapshots/{node_id}/cache/stats");
    initEndpoint(routes, "async_search.submit", true, "/_async_search", "/{index}/_async_search");
    initEndpoint(routes, "rollup.get_jobs", false, "/_rollup/job/{id}", "/_rollup/job");
    initEndpoint(
        routes,
        "ml.revert_model_snapshot",
        false,
        "/_ml/anomaly_detectors/{job_id}/model_snapshots/{snapshot_id}/_revert");
    initEndpoint(routes, "transform.delete_transform", false, "/_transform/{transform_id}");
    initEndpoint(routes, "cluster.pending_tasks", false, "/_cluster/pending_tasks");
    initEndpoint(
        routes,
        "ml.get_model_snapshot_upgrade_stats",
        false,
        "/_ml/anomaly_detectors/{job_id}/model_snapshots/{snapshot_id}/_upgrade/_stats");
    initEndpoint(
        routes,
        "ml.get_categories",
        false,
        "/_ml/anomaly_detectors/{job_id}/results/categories/{category_id}",
        "/_ml/anomaly_detectors/{job_id}/results/categories");
    initEndpoint(routes, "ccr.pause_follow", false, "/{index}/_ccr/pause_follow");
    initEndpoint(routes, "security.authenticate", false, "/_security/_authenticate");
    initEndpoint(routes, "enrich.stats", false, "/_enrich/_stats");
    initEndpoint(
        routes,
        "ml.put_trained_model_alias",
        false,
        "/_ml/trained_models/{model_id}/model_aliases/{model_alias}");
    initEndpoint(
        routes,
        "ml.get_overall_buckets",
        false,
        "/_ml/anomaly_detectors/{job_id}/results/overall_buckets");
    initEndpoint(routes, "indices.get_template", false, "/_template", "/_template/{name}");
    initEndpoint(routes, "security.delete_role_mapping", false, "/_security/role_mapping/{name}");
    initEndpoint(
        routes, "ml.get_datafeeds", false, "/_ml/datafeeds/{datafeed_id}", "/_ml/datafeeds");
    initEndpoint(routes, "slm.execute_lifecycle", false, "/_slm/policy/{policy_id}/_execute");
    initEndpoint(routes, "close_point_in_time", false, "/_pit");
    initEndpoint(routes, "snapshot.cleanup_repository", false, "/_snapshot/{repository}/_cleanup");
    initEndpoint(
        routes, "autoscaling.get_autoscaling_policy", false, "/_autoscaling/policy/{name}");
    initEndpoint(routes, "slm.put_lifecycle", false, "/_slm/policy/{policy_id}");
    initEndpoint(
        routes, "ml.get_jobs", false, "/_ml/anomaly_detectors/{job_id}", "/_ml/anomaly_detectors");
    initEndpoint(
        routes,
        "ml.get_trained_models_stats",
        false,
        "/_ml/trained_models/{model_id}/_stats",
        "/_ml/trained_models/_stats");
    initEndpoint(
        routes, "ml.validate_detector", false, "/_ml/anomaly_detectors/_validate/detector");
    initEndpoint(routes, "watcher.put_watch", false, "/_watcher/watch/{id}");
    initEndpoint(routes, "transform.update_transform", false, "/_transform/{transform_id}/_update");
    initEndpoint(routes, "ml.post_calendar_events", false, "/_ml/calendars/{calendar_id}/events");
    initEndpoint(
        routes, "migration.get_feature_upgrade_status", false, "/_migration/system_features");
    initEndpoint(routes, "get_script_context", false, "/_script_context");
    initEndpoint(routes, "ml.put_filter", false, "/_ml/filters/{filter_id}");
    initEndpoint(routes, "ml.update_job", false, "/_ml/anomaly_detectors/{job_id}/_update");
    initEndpoint(routes, "ingest.geo_ip_stats", false, "/_ingest/geoip/stats");
    initEndpoint(routes, "security.delete_user", false, "/_security/user/{username}");
    initEndpoint(routes, "indices.unfreeze", false, "/{index}/_unfreeze");
    initEndpoint(routes, "snapshot.create_repository", false, "/_snapshot/{repository}");
    initEndpoint(
        routes,
        "cluster.get_component_template",
        false,
        "/_component_template",
        "/_component_template/{name}");
    initEndpoint(routes, "ilm.migrate_to_data_tiers", false, "/_ilm/migrate_to_data_tiers");
    initEndpoint(routes, "indices.refresh", false, "/_refresh", "/{index}/_refresh");
    initEndpoint(
        routes, "ml.get_calendars", false, "/_ml/calendars", "/_ml/calendars/{calendar_id}");
    initEndpoint(
        routes, "watcher.deactivate_watch", false, "/_watcher/watch/{watch_id}/_deactivate");
    initEndpoint(routes, "cluster.health", false, "/_cluster/health", "/_cluster/health/{index}");
    initEndpoint(
        routes, "dangling_indices.delete_dangling_index", false, "/_dangling/{index_uuid}");
    initEndpoint(routes, "health_report", false, "/_health_report", "/_health_report/{feature}");
    initEndpoint(routes, "watcher.query_watches", false, "/_watcher/_query/watches");
    initEndpoint(routes, "ccr.unfollow", false, "/{index}/_ccr/unfollow");
    initEndpoint(routes, "ml.validate", false, "/_ml/anomaly_detectors/_validate");
    initEndpoint(routes, "cat.plugins", false, "/_cat/plugins");
    initEndpoint(
        routes,
        "watcher.execute_watch",
        false,
        "/_watcher/watch/{id}/_execute",
        "/_watcher/watch/_execute");
    initEndpoint(routes, "search_shards", false, "/_search_shards", "/{index}/_search_shards");
    initEndpoint(routes, "cat.shards", false, "/_cat/shards", "/_cat/shards/{index}");
    initEndpoint(routes, "ml.delete_job", false, "/_ml/anomaly_detectors/{job_id}");
    initEndpoint(routes, "ilm.start", false, "/_ilm/start");
    initEndpoint(routes, "security.get_user_profile", false, "/_security/profile/{uid}");
    initEndpoint(routes, "indices.modify_data_stream", false, "/_data_stream/_modify");
    initEndpoint(routes, "indices.exists_alias", false, "/_alias/{name}", "/{index}/_alias/{name}");
    initEndpoint(routes, "rollup.stop_job", false, "/_rollup/job/{id}/_stop");
    initEndpoint(routes, "dangling_indices.list_dangling_indices", false, "/_dangling");
    initEndpoint(routes, "snapshot.delete", false, "/_snapshot/{repository}/{snapshot}");
    initEndpoint(routes, "security.activate_user_profile", false, "/_security/profile/_activate");
    initEndpoint(
        routes,
        "ml.start_trained_model_deployment",
        false,
        "/_ml/trained_models/{model_id}/deployment/_start");
    initEndpoint(routes, "transform.start_transform", false, "/_transform/{transform_id}/_start");
    initEndpoint(routes, "cat.repositories", false, "/_cat/repositories");
    initEndpoint(routes, "ilm.get_status", false, "/_ilm/status");
    initEndpoint(routes, "shutdown.delete_node", false, "/_nodes/{node_id}/shutdown");
    initEndpoint(
        routes,
        "nodes.stats",
        false,
        "/_nodes/stats",
        "/_nodes/{node_id}/stats",
        "/_nodes/stats/{metric}",
        "/_nodes/{node_id}/stats/{metric}",
        "/_nodes/stats/{metric}/{index_metric}",
        "/_nodes/{node_id}/stats/{metric}/{index_metric}");
    initEndpoint(routes, "get_script_languages", false, "/_script_language");
    initEndpoint(routes, "slm.execute_retention", false, "/_slm/_execute_retention");
    initEndpoint(
        routes,
        "security.get_service_accounts",
        false,
        "/_security/service/{namespace}/{service}",
        "/_security/service/{namespace}",
        "/_security/service");
    initEndpoint(routes, "shutdown.put_node", false, "/_nodes/{node_id}/shutdown");
    initEndpoint(routes, "indices.resolve_index", false, "/_resolve/index/{name}");
    initEndpoint(routes, "search", true, "/_search", "/{index}/_search");
    initEndpoint(routes, "sql.get_async", false, "/_sql/async/{id}");
    initEndpoint(
        routes, "delete_by_query_rethrottle", false, "/_delete_by_query/{task_id}/_rethrottle");
    initEndpoint(
        routes, "transform.get_transform", false, "/_transform/{transform_id}", "/_transform");
    initEndpoint(routes, "security.invalidate_api_key", false, "/_security/api_key");
    initEndpoint(routes, "security.saml_prepare_authentication", false, "/_security/saml/prepare");
    initEndpoint(
        routes, "ml.get_memory_stats", false, "/_ml/memory/_stats", "/_ml/memory/{node_id}/_stats");
    initEndpoint(routes, "ccr.stats", false, "/_ccr/stats");
    initEndpoint(routes, "indices.forcemerge", false, "/_forcemerge", "/{index}/_forcemerge");
    initEndpoint(routes, "indices.delete_template", false, "/_template/{name}");
    initEndpoint(routes, "sql.delete_async", false, "/_sql/async/delete/{id}");
    initEndpoint(routes, "security.update_api_key", false, "/_security/api_key/{id}");
    initEndpoint(
        routes,
        "security.create_service_token",
        false,
        "/_security/service/{namespace}/{service}/credential/token/{name}",
        "/_security/service/{namespace}/{service}/credential/token");
    initEndpoint(routes, "license.get_trial_status", false, "/_license/trial_status");
    initEndpoint(
        routes, "searchable_snapshots.mount", false, "/_snapshot/{repository}/{snapshot}/_mount");
    initEndpoint(routes, "security.grant_api_key", false, "/_security/api_key/grant");
    initEndpoint(routes, "ilm.retry", false, "/{index}/_ilm/retry");
    initEndpoint(routes, "ml.reset_job", false, "/_ml/anomaly_detectors/{job_id}/_reset");
    initEndpoint(routes, "ml.close_job", false, "/_ml/anomaly_detectors/{job_id}/_close");
    initEndpoint(
        routes,
        "ml.explain_data_frame_analytics",
        false,
        "/_ml/data_frame/analytics/_explain",
        "/_ml/data_frame/analytics/{id}/_explain");
    initEndpoint(
        routes,
        "security.clear_cached_service_tokens",
        false,
        "/_security/service/{namespace}/{service}/credential/token/{name}/_clear_cache");
    initEndpoint(routes, "search_mvt", false, "/{index}/_mvt/{field}/{zoom}/{x}/{y}");
    routesMap = Collections.unmodifiableMap(routes);
  }

  private ElasticsearchEndpointMap() {}

  private static void initEndpoint(
      Map<String, ElasticsearchEndpointDefinition> map,
      String endpointId,
      boolean isSearchEndpoint,
      String... routes) {
    ElasticsearchEndpointDefinition endpointDef =
        new ElasticsearchEndpointDefinition(endpointId, routes, isSearchEndpoint);
    map.put(endpointId, endpointDef);
  }

  @Nullable
  public static ElasticsearchEndpointDefinition get(String endpointId) {
    return routesMap.get(endpointId);
  }

  public static Collection<ElasticsearchEndpointDefinition> getAllEndpoints() {
    return routesMap.values();
  }
}
