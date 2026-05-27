#!/usr/bin/env ruby

require 'yaml'

failures = []

def resource_value(container, category, name)
  container.dig('resources', category, name)
end

Dir['k8s/*.yml'].sort.each do |path|
  YAML.load_stream(File.read(path)).compact.each do |document|
    next unless document['kind'] == 'Deployment'

    deployment = document.fetch('metadata').fetch('name')
    template = document.dig('spec', 'template') || {}
    annotations = template.dig('metadata', 'annotations') || {}
    pod_spec = template['spec'] || {}
    pod_context = pod_spec['securityContext'] || {}
    run_as_non_root_exception = annotations['petclinic.spring.io/run-as-non-root-exception'].to_s != ''
    read_only_root_exception = annotations['petclinic.spring.io/read-only-root-filesystem-exception'].to_s != ''

    unless pod_context['runAsNonRoot'] == true || run_as_non_root_exception
      failures << "#{path}: Deployment/#{deployment} must set pod securityContext.runAsNonRoot=true"
    end

    unless pod_context.dig('seccompProfile', 'type') == 'RuntimeDefault'
      failures << "#{path}: Deployment/#{deployment} must set pod securityContext.seccompProfile.type=RuntimeDefault"
    end

    Array(pod_spec['containers']).each do |container|
      name = container.fetch('name')
      context = container['securityContext'] || {}

      unless context['allowPrivilegeEscalation'] == false
        failures << "#{path}: Deployment/#{deployment} container/#{name} must set allowPrivilegeEscalation=false"
      end

      unless Array(context.dig('capabilities', 'drop')).include?('ALL')
        failures << "#{path}: Deployment/#{deployment} container/#{name} must drop ALL Linux capabilities"
      end

      unless context['readOnlyRootFilesystem'] == true || read_only_root_exception
        failures << "#{path}: Deployment/#{deployment} container/#{name} must set readOnlyRootFilesystem=true"
      end

      %w[cpu memory].each do |resource|
        if resource_value(container, 'requests', resource).to_s.empty?
          failures << "#{path}: Deployment/#{deployment} container/#{name} must set resources.requests.#{resource}"
        end

        if resource_value(container, 'limits', resource).to_s.empty?
          failures << "#{path}: Deployment/#{deployment} container/#{name} must set resources.limits.#{resource}"
        end
      end
    end
  end
end

unless failures.empty?
  warn "Kubernetes manifests are missing required runtime hardening:"
  failures.each { |failure| warn "  - #{failure}" }
  exit 1
end
